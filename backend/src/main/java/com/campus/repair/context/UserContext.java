package com.campus.repair.context;

/**
 * 当前请求用户上下文 (ThreadLocal)
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> ROLE = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();

    public static void set(Long userId, Integer role, String username) {
        USER_ID.set(userId);
        ROLE.set(role);
        USERNAME.set(username);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static Integer getRole() {
        return ROLE.get();
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    /** 请求结束时调用，防止内存泄漏 */
    public static void remove() {
        USER_ID.remove();
        ROLE.remove();
        USERNAME.remove();
    }
}
