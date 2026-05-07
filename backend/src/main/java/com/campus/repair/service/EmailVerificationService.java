package com.campus.repair.service;

/**
 * 邮箱验证码：注册 / 邮箱登录 / 忘记密码重置；存 Redis；QQ SMTP 可配，未配时仅打日志。
 */
public interface EmailVerificationService {

    void sendCode(String email, String scene);

    void verifyAndConsume(String email, String scene, String code);
}
