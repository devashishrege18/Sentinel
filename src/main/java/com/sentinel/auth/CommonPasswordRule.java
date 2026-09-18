package com.sentinel.auth;

import java.util.Set;

/**
 * Rejects passwords that appear (case-insensitively) in a small built-in
 * list of extremely common passwords. This is intentionally a short,
 * illustrative list rather than a full breach-corpus lookup.
 */
public class CommonPasswordRule implements PasswordRule {

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password", "123456", "12345678", "qwerty", "abc123",
            "letmein", "monkey", "111111", "iloveyou", "admin",
            "welcome", "password1", "123456789", "football", "dragon"
    );

    @Override
    public RuleResult evaluate(String password) {
        boolean isCommon = COMMON_PASSWORDS.contains(password.toLowerCase());
        String message = isCommon
                ? "Matches a known common password"
                : "Not found in the common password list";
        return new RuleResult(!isCommon, message);
    }

    @Override
    public String getName() {
        return "Common Password Check";
    }
}
