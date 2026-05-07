package com.campus.repair.service.impl;

import com.campus.repair.common.BusinessException;
import com.campus.repair.service.EmailVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * scene: REGISTER / LOGIN / RESET_PASSWORD
 * <p>验证码默认存 Redis；若 Redis 不可用，自动降级为进程内内存（单机毕设/验收可完成注册，生产多实例需 Redis）。</p>
 */
@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationServiceImpl.class);
    private static final Pattern SCENE = Pattern.compile("^(REGISTER|LOGIN|RESET_PASSWORD)$");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final int CODE_TTL_MIN = 5;
    private static final int SEND_INTERVAL_SEC = 60;

    private static final String CODE_KEY = "auth:email:code:%s:%s";
    private static final String RATE_KEY = "auth:email:rate:%s:%s";

    private static final AtomicBoolean MEM_FALLBACK_WARNED = new AtomicBoolean(false);

    private static final class MemEntry {
        final String value;
        final long expireAtMs;

        MemEntry(String value, long expireAtMs) {
            this.value = value;
            this.expireAtMs = expireAtMs;
        }
    }

    private final ConcurrentHashMap<String, MemEntry> memStore = new ConcurrentHashMap<>();

    private final StringRedisTemplate redis;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public EmailVerificationServiceImpl(StringRedisTemplate redis,
                                        ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.redis = redis;
        this.mailSenderProvider = mailSenderProvider;
    }

    @Override
    public void sendCode(String email, String scene) {
        String em = normalizeEmail(email);
        String sc = requireScene(scene);
        if (!EMAIL.matcher(em).matches()) {
            throw new BusinessException("邮箱格式不正确");
        }
        String rateKey = String.format(RATE_KEY, sc, em);
        if (isRateLimited(rateKey)) {
            throw new BusinessException("发送过频，请稍后再试");
        }
        String code = String.format("%06d", (int) (Math.random() * 1_000_000));
        String codeKey = String.format(CODE_KEY, sc, em);
        long codeTtlMs = TimeUnit.MINUTES.toMillis(CODE_TTL_MIN);
        long rateTtlMs = TimeUnit.SECONDS.toMillis(SEND_INTERVAL_SEC);
        setCodeAndRateKeys(codeKey, code, codeTtlMs, rateKey, rateTtlMs);

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender != null && StringUtils.hasText(mailFrom)) {
            try {
                SimpleMailMessage m = new SimpleMailMessage();
                m.setFrom(mailFrom);
                m.setTo(em);
                m.setSubject("校园报修 - 邮箱验证码");
                m.setText("【校园报修】您的验证码为 " + code + " ，" + CODE_TTL_MIN + " 分钟内有效。如非本人操作请忽略。\n场景：" + sc);
                mailSender.send(m);
            } catch (Exception e) {
                log.error("发送邮件失败，验证码已写入缓存/内存: {}", e.getMessage());
                log.warn("[邮箱验证码-备用] 收件人={} 场景={} 验证码={}", em, sc, code);
            }
        } else {
            log.warn("[未配置发信邮箱] 请配置 spring.mail.username 与 smtp 授权码。验证码: {} -> {}", em, code);
        }
    }

    @Override
    public void verifyAndConsume(String email, String scene, String code) {
        String em = normalizeEmail(email);
        String sc = requireScene(scene);
        if (code == null) {
            throw new BusinessException("请填写邮箱验证码");
        }
        String c = code.trim();
        if (c.length() < 4 || c.length() > 8) {
            throw new BusinessException("验证码格式无效");
        }
        String codeKey = String.format(CODE_KEY, sc, em);
        String saved = getCode(codeKey);
        if (saved == null) {
            throw new BusinessException("验证码已过期或未发送，请重试");
        }
        if (!saved.equals(c)) {
            throw new BusinessException("验证码错误");
        }
        deleteKey(codeKey);
    }

    private boolean isRateLimited(String rateKey) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(rateKey));
        } catch (Exception e) {
            warnMemFallback(e);
            return memGet(rateKey) != null;
        }
    }

    private void setCodeAndRateKeys(String codeKey, String code, long codeTtlMs, String rateKey, long rateTtlMs) {
        try {
            redis.opsForValue().set(codeKey, code, CODE_TTL_MIN, TimeUnit.MINUTES);
            redis.opsForValue().set(rateKey, "1", SEND_INTERVAL_SEC, TimeUnit.SECONDS);
        } catch (Exception e) {
            warnMemFallback(e);
            memSet(codeKey, code, System.currentTimeMillis() + codeTtlMs);
            memSet(rateKey, "1", System.currentTimeMillis() + rateTtlMs);
        }
    }

    private String getCode(String codeKey) {
        try {
            return redis.opsForValue().get(codeKey);
        } catch (Exception e) {
            warnMemFallback(e);
            return memGet(codeKey);
        }
    }

    private void deleteKey(String key) {
        try {
            redis.delete(key);
        } catch (Exception e) {
            memDelete(key);
        }
    }

    private void memSet(String key, String value, long expireAtMs) {
        memStore.put(key, new MemEntry(value, expireAtMs));
    }

    private String memGet(String key) {
        MemEntry c = memStore.get(key);
        if (c == null) {
            return null;
        }
        if (System.currentTimeMillis() > c.expireAtMs) {
            memStore.remove(key, c);
            return null;
        }
        return c.value;
    }

    private void memDelete(String key) {
        memStore.remove(key);
    }

    private void warnMemFallback(Exception e) {
        if (MEM_FALLBACK_WARNED.compareAndSet(false, true)) {
            log.warn("Redis 不可用，邮箱验证码将使用本进程内存（单机可用；多实例/生产请启动 Redis 并核对密码）: {}", e.getMessage());
        }
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase();
    }

    private static String requireScene(String scene) {
        if (scene == null) {
            throw new BusinessException("参数 scene 无效");
        }
        String s = scene.trim().toUpperCase();
        if (!SCENE.matcher(s).matches()) {
            throw new BusinessException("参数 scene 仅支持 REGISTER、LOGIN 或 RESET_PASSWORD");
        }
        return s;
    }
}
