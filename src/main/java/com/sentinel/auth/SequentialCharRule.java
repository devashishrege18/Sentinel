package com.sentinel.auth;

/**
 * Flags simple, predictable patterns: runs of ascending/descending
 * characters (e.g. "abcd", "4321") and runs of a single repeated
 * character (e.g. "aaaa"), each of a configurable minimum run length.
 */
public class SequentialCharRule implements PasswordRule {

    private final int minRunLength;

    public SequentialCharRule(int minRunLength) {
        this.minRunLength = minRunLength;
    }

    @Override
    public RuleResult evaluate(String password) {
        if (hasSequentialRun(password) || hasRepeatedRun(password)) {
            return new RuleResult(false, "Contains a predictable sequential or repeated pattern");
        }
        return new RuleResult(true, "No obvious sequential or repeated patterns found");
    }

    private boolean hasSequentialRun(String password) {
        int ascendingRun = 1;
        int descendingRun = 1;
        for (int i = 1; i < password.length(); i++) {
            int diff = password.charAt(i) - password.charAt(i - 1);
            ascendingRun = (diff == 1) ? ascendingRun + 1 : 1;
            descendingRun = (diff == -1) ? descendingRun + 1 : 1;
            if (ascendingRun >= minRunLength || descendingRun >= minRunLength) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRepeatedRun(String password) {
        int repeatRun = 1;
        for (int i = 1; i < password.length(); i++) {
            repeatRun = (password.charAt(i) == password.charAt(i - 1)) ? repeatRun + 1 : 1;
            if (repeatRun >= minRunLength) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return "Sequential/Repeated Pattern Check";
    }
}
