package com.campus.repair.controller;

import com.campus.repair.common.BusinessException;
import com.campus.repair.common.Result;
import com.campus.repair.controller.dto.LoginRequest;
import com.campus.repair.controller.dto.LoginResponse;
import com.campus.repair.controller.dto.RegisterRequest;
import com.campus.repair.controller.dto.SendCodeRequest;
import com.campus.repair.controller.dto.SendEmailCodeRequest;
import com.campus.repair.controller.dto.LoginEmailRequest;
import com.campus.repair.controller.dto.ResetPasswordRequest;
import com.campus.repair.entity.SysUser;
import com.campus.repair.service.EmailVerificationService;
import com.campus.repair.service.UserService;
import com.campus.repair.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import com.campus.repair.annotation.OperationLog;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 认证控制器 - 登录、登出
 * Redis 不可用时自动降级为仅使用 JWT，不阻断登录流程
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String REDIS_TOKEN_PREFIX = "auth:token:";
    private static final long TOKEN_EXPIRE_HOURS = 2;

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    /** 仅首次 Redis 失败时打 WARN，避免控制台刷屏 */
    private static final AtomicBoolean redisFailWarned = new AtomicBoolean(false);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final EmailVerificationService emailVerificationService;

    public AuthController(UserService userService,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          StringRedisTemplate redisTemplate,
                          EmailVerificationService emailVerificationService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/login")
    @OperationLog("login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        SysUser user;
        try {
            user = userService.getByUsername(request.getUsername());
            if (user == null) {
                throw new BusinessException("用户名或密码错误");
            }
            if (user.getStatus() != null && user.getStatus() == 0) {
                throw new BusinessException("账号已被禁用");
            }
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new BusinessException("用户名或密码错误");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("登录过程出现异常: {}", e.getMessage(), e);
            throw new BusinessException("系统繁忙，请稍后重试");
        }

        return Result.success(buildLoginResponse(user));
    }

    @PostMapping("/login-email")
    @OperationLog("login-email")
    public Result<LoginResponse> loginByEmail(@Valid @RequestBody LoginEmailRequest request) {
        String em = request.getEmail().trim().toLowerCase();
        SysUser user = userService.getByEmail(em);
        if (user == null) {
            throw new BusinessException("该邮箱未绑定账号");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        emailVerificationService.verifyAndConsume(em, "LOGIN", request.getCode());
        return Result.success(buildLoginResponse(user));
    }

    private LoginResponse buildLoginResponse(SysUser user) {
        Integer role = user.getRole() != null ? user.getRole() : 0;
        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), role);
        String redisKey = REDIS_TOKEN_PREFIX + user.getUserId();
        try {
            redisTemplate.opsForValue().set(redisKey, token, TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            if (redisFailWarned.compareAndSet(false, true)) {
                log.warn("Redis 不可用，Token 将仅使用 JWT 校验。若需 Redis 存储，请启动 Redis 服务。{}", e.getMessage());
            }
        }
        String displayName = user.getNickname() != null && !user.getNickname().isEmpty()
                ? user.getNickname() : user.getRealName();
        return new LoginResponse(
                token,
                user.getUserId(),
                user.getUsername(),
                displayName,
                role
        );
    }

    @PostMapping("/register")
    @OperationLog("register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        try {
            String email = request.getEmail().trim().toLowerCase();
            // 先占坑校验，再消费验证码，避免验码成功后因重名需重发
            SysUser existingUser = userService.getByUsername(request.getUsername());
            if (existingUser != null) {
                throw new BusinessException("用户名已存在");
            }
            SysUser existingPhoneUser = userService.getByPhone(request.getPhone());
            if (existingPhoneUser != null) {
                throw new BusinessException("手机号已被注册");
            }
            if (userService.getByEmail(email) != null) {
                throw new BusinessException("该邮箱已被注册");
            }
            emailVerificationService.verifyAndConsume(email, "REGISTER", request.getEmailCode());
            // 公开注册仅允许学生（XQWD：维修工由管理员在后台创建，不可自助注册为维修工）
            SysUser user = new SysUser();
            user.setUsername(request.getUsername().trim());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRealName(request.getRealName().trim());
            user.setNickname(request.getRealName().trim());
            user.setPhone(request.getPhone().trim());
            user.setEmail(email);
            user.setRole(0);
            user.setDepartment(request.getDepartment().trim());
            user.setStatus(1);
            user.setIsDeleted(0);
            user.setSignature(null);
            user.setGender(0);
            userService.save(user);
            return Result.success();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("注册过程出现异常: {}", e.getMessage());
            throw new BusinessException("系统繁忙，请稍后重试");
        }
    }

    @PostMapping("/send-email-code")
    @OperationLog("send-email-code")
    public Result<Void> sendEmailCode(@Valid @RequestBody SendEmailCodeRequest request) {
        String em = request.getEmail().trim().toLowerCase();
        String sc = request.getScene().trim().toUpperCase();
        if ("REGISTER".equals(sc)) {
            if (userService.getByEmail(em) != null) {
                throw new BusinessException("该邮箱已注册，请直接登录或更换邮箱");
            }
        } else if ("LOGIN".equals(sc)) {
            if (userService.getByEmail(em) == null) {
                throw new BusinessException("该邮箱未绑定账号，请先注册或使用账号密码登录");
            }
        } else if ("RESET_PASSWORD".equals(sc)) {
            String un = request.getUsername();
            if (!StringUtils.hasText(un)) {
                throw new BusinessException("请填写用户名");
            }
            SysUser u = userService.getByUsername(un.trim());
            if (u == null) {
                throw new BusinessException("用户不存在");
            }
            if (!StringUtils.hasText(u.getEmail())) {
                throw new BusinessException("该账号未绑定邮箱，无法通过邮箱重置，请联系管理员绑定邮箱");
            }
            if (!em.equals(u.getEmail().trim().toLowerCase())) {
                throw new BusinessException("邮箱与用户名不匹配，请核对后重试");
            }
        }
        emailVerificationService.sendCode(em, sc);
        return Result.success();
    }

    @PostMapping("/send-code")
    @OperationLog("send-code")
    public Result<Void> sendCode(@RequestBody SendCodeRequest request) {
        try {
            // 检查手机号是否存在
            SysUser user = userService.getByPhone(request.getPhone());
            if (user == null) {
                throw new BusinessException("手机号未注册");
            }
            // 生成验证码
            String code = String.format("%06d", new java.util.Random().nextInt(999999));
            // 这里应该调用短信服务发送验证码，现在只是模拟
            log.info("验证码: {} 已发送到手机: {}", code, request.getPhone());
            // 实际项目中应该将验证码存储到 Redis 中，设置过期时间
            return Result.success();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("发送验证码过程出现异常: {}", e.getMessage());
            throw new BusinessException("系统繁忙，请稍后重试");
        }
    }

    @PostMapping("/reset-password")
    @OperationLog("reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            SysUser user = userService.getByUsername(request.getUsername().trim());
            if (user == null) {
                throw new BusinessException("用户不存在");
            }
            String em = request.getEmail().trim().toLowerCase();
            if (!StringUtils.hasText(user.getEmail())) {
                throw new BusinessException("该账号未绑定邮箱，无法通过邮箱重置");
            }
            if (!em.equals(user.getEmail().trim().toLowerCase())) {
                throw new BusinessException("邮箱与用户名不匹配");
            }
            emailVerificationService.verifyAndConsume(em, "RESET_PASSWORD", request.getCode());
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            user.setUpdateTime(LocalDateTime.now());
            userService.updateById(user);
            return Result.success();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("重置密码过程出现异常: {}", e.getMessage());
            throw new BusinessException("系统繁忙，请稍后重试");
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            Long userId = (Long) auth.getPrincipal();
            try {
                redisTemplate.delete(REDIS_TOKEN_PREFIX + userId);
            } catch (Exception e) {
                if (redisFailWarned.compareAndSet(false, true)) {
                    log.warn("Redis 不可用，登出将仅清除本地状态。若需 Redis，请启动 Redis 服务。{}", e.getMessage());
                }
            }
        }
        return Result.success();
    }
}
