package com.sentinel.auth;

public class LengthRule implements PasswordRule {

    private final int minLength;

    public LengthRule(int minLength) {
        this.minLength = minLength;
    }

    @Override
    public RuleResult evaluate(String password) {
        boolean passed = password.length() >= minLength;
        String message = passed
                ? "Length OK (" + password.length() + " characters)"
                : "Too short: " + password.length() + " characters (minimum " + minLength + ")";
        return new RuleResult(passed, message);
    }

    @Override
    public String getName() {
        return "Minimum Length";
    }
}
