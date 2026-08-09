package com.techvalley.client.security;

public class UserContext {
    private static final ThreadLocal<UserContextInfo> CONTEXT = new ThreadLocal<>();

    public static void set(UserContextInfo user) {
        CONTEXT.set(user);
    }

    public static UserContextInfo get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
