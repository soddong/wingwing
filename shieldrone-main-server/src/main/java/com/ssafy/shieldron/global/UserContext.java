package com.ssafy.shieldron.global;

public class UserContext {
    private static final ThreadLocal<String> phoneNumberHolder = new ThreadLocal<>();

    public static void setPhoneNumber(String phoneNumber) {
        phoneNumberHolder.set(phoneNumber);
    }

    public static String getPhoneNumber() {
        return phoneNumberHolder.get();
    }

    public static void clear() {
        phoneNumberHolder.remove();
    }
}
