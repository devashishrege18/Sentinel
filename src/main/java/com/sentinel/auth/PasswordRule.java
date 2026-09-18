package com.sentinel.auth;

/**
 * Strategy interface: a single, independent rule that scores or checks one
 * aspect of password strength. New rules can be added without touching
 * {@link PasswordAuditor} or any existing rule.
 */
public interface PasswordRule {

    /**
     * @param password the candidate password
     * @return the outcome of evaluating just this rule
     */
    RuleResult evaluate(String password);

    /**
     * Human-readable name of the rule, used in report output.
     */
    String getName();

    /**
     * Simple value holder for one rule's verdict: whether it passed and a
     * short explanation shown to the user.
     */
    final class RuleResult {
        private final boolean passed;
        private final String message;

        public RuleResult(boolean passed, String message) {
            this.passed = passed;
            this.message = message;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getMessage() {
            return message;
        }
    }
}
