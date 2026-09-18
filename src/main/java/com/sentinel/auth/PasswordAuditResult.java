package com.sentinel.auth;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Aggregated outcome of running every configured {@link PasswordRule}
 * against one password: a pass/fail per rule plus an overall strength score.
 */
public final class PasswordAuditResult {

    private final Map<String, PasswordRule.RuleResult> ruleResults;
    private final int score;
    private final int maxScore;
    private final LocalDateTime auditedAt;

    public PasswordAuditResult(Map<String, PasswordRule.RuleResult> ruleResults) {
        this.ruleResults = ruleResults;
        this.maxScore = ruleResults.size();
        this.score = (int) ruleResults.values().stream()
                .filter(PasswordRule.RuleResult::isPassed)
                .count();
        this.auditedAt = LocalDateTime.now();
    }

    public Map<String, PasswordRule.RuleResult> getRuleResults() {
        return ruleResults;
    }

    public int getScore() {
        return score;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public LocalDateTime getAuditedAt() {
        return auditedAt;
    }

    public String getStrengthLabel() {
        double ratio = (double) score / maxScore;
        if (ratio == 1.0) return "STRONG";
        if (ratio >= 0.75) return "MODERATE";
        if (ratio >= 0.5) return "WEAK";
        return "VERY WEAK";
    }

    public List<String> getFailureMessages() {
        return ruleResults.entrySet().stream()
                .filter(e -> !e.getValue().isPassed())
                .map(e -> e.getKey() + ": " + e.getValue().getMessage())
                .toList();
    }
}
