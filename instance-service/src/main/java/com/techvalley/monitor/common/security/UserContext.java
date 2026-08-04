package com.techvalley.monitor.common.security;

public class UserContext {
    private static final ThreadLocal<UserContextInfo> CONTEXT = new ThreadLocal<>();

    public static void set(UserContextInfo info) {
        CONTEXT.set(info);
    }

    public static UserContextInfo get() {
        UserContextInfo info = CONTEXT.get();
        if (info == null) {
            return new UserContextInfo(1L, "admin@techvalley.com", "ADMIN");
        }
        return info;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
