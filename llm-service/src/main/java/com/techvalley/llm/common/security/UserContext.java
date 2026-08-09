package com.techvalley.llm.common.security;

public class UserContext {
    private static final ThreadLocal<UserContextInfo> CONTEXT = new ThreadLocal<>();

    public static void set(UserContextInfo info) {
        CONTEXT.set(info);
    }

    public static UserContextInfo get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}