package com.sentinel.auth;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs a password through every configured {@link PasswordRule} and
 * aggregates the results. The set of rules is injected, so the auditor
 * itself has no knowledge of what "strong" means beyond "every rule
 * passed" - new policies are added purely by supplying different rules.
 */
public class PasswordAuditor {

    private final List<PasswordRule> rules;

    public PasswordAuditor(List<PasswordRule> rules) {
        this.rules = rules;
    }

    /**
     * The default rule set: reasonable general-purpose password policy.
     */
    public static PasswordAuditor withDefaultRules() {
        return new PasswordAuditor(List.of(
                new LengthRule(12),
                new ComplexityRule(3),
                new CommonPasswordRule(),
                new SequentialCharRule(4)
        ));
    }

    public PasswordAuditResult audit(String password) {
        Map<String, PasswordRule.RuleResult> results = new LinkedHashMap<>();
        for (PasswordRule rule : rules) {
            results.put(rule.getName(), rule.evaluate(password));
        }
        return new PasswordAuditResult(results);
    }
}
