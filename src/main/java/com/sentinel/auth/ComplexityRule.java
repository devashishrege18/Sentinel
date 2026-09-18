package com.sentinel.auth;

import java.util.regex.Pattern;

/**
 * Requires a minimum number of distinct character classes to be present:
 * lowercase, uppercase, digit, and symbol.
 */
public class ComplexityRule implements PasswordRule {

    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SYMBOL = Pattern.compile("[^a-zA-Z0-9]");

    private final int minClasses;

    public ComplexityRule(int minClasses) {
        this.minClasses = minClasses;
    }

    @Override
    public RuleResult evaluate(String password) {
        int classesPresent = 0;
        classesPresent += LOWERCASE.matcher(password).find() ? 1 : 0;
        classesPresent += UPPERCASE.matcher(password).find() ? 1 : 0;
        classesPresent += DIGIT.matcher(password).find() ? 1 : 0;
        classesPresent += SYMBOL.matcher(password).find() ? 1 : 0;

        boolean passed = classesPresent >= minClasses;
        String message = passed
                ? "Uses " + classesPresent + " character classes"
                : "Only uses " + classesPresent + " character classes (need at least " + minClasses
                    + ": lowercase, uppercase, digit, symbol)";
        return new RuleResult(passed, message);
    }

    @Override
    public String getName() {
        return "Character Complexity";
    }
}
