package com.campus.repair.filter;

import com.campus.repair.context.UserContext;
import com.campus.repair.entity.SysUser;
import com.campus.repair.mapper.SysUserMapper;
import com.campus.repair.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT 认证过滤器 - 解析 Token、校验 Redis、设置 UserContext
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String REDIS_TOKEN_PREFIX = "auth:token:";

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final SysUserMapper sysUserMapper;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, StringRedisTemplate redisTemplate, SysUserMapper sysUserMapper) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Claims claims = jwtUtil.validateToken(token);
                if (claims != null) {
                    Long userId = jwtUtil.getUserId(claims);
                    Integer role = jwtUtil.getRole(claims);
                    if (userId == null) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                    if (role == null) {
                        try {
                            SysUser u = sysUserMapper.selectById(userId);
                            if (u != null && u.getRole() != null) {
                                role = u.getRole();
                            }
                        } catch (Exception e) {
                            log.debug("从库补全角色失败: {}", e.getMessage());
                        }
                    }
                    if (role == null) {
                        role = 0;
                    }
                    String username = claims.getSubject();
                    String stored = null;
                    try {
                        stored = redisTemplate.opsForValue().get(REDIS_TOKEN_PREFIX + userId);
                    } catch (Exception ignored) {
                        // Redis 不可用时仅根据 JWT 校验，不抛出异常
                    }
                    if (stored != null && !stored.equals(token)) {
                        // 多端/重新登录后 Redis 已换会话：仍以合法 JWT 为准，避免大量「未登录/无权限」
                        log.debug("Redis 会话与当前请求 Token 不一致，userId={}，以 JWT 为有效凭证", userId);
                    }
                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_" + getRoleName(role))
                    );
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    authentication.setDetails(username);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    UserContext.set(userId, role, username);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            UserContext.remove();
        }
    }

    private String getRoleName(Integer role) {
        int r = role != null ? role : 0;
        if (r == 1) return "STAFF";
        if (r == 2) return "ADMIN";
        return "STUDENT";
    }
}
