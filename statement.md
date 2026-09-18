# Sentinel — Local Security Monitoring Toolkit

## Problem Statement

Modern computers can expose security weaknesses through unnecessary open network ports, unauthorized file modifications, and weak passwords. Many security tools are either specialized, infrastructure-heavy, or difficult to study as a single Java application.

Sentinel addresses this problem by providing a lightweight Java command-line toolkit that combines TCP port scanning, SHA-256 file integrity monitoring, password-strength auditing, local SQLite persistence, and consolidated reporting.

## Scope

The project covers local security auditing and monitoring. It can scan a permitted host for TCP port states, create and compare file-integrity baselines, audit password strength against four rules, persist security-event summaries, and generate a combined report.

The port scanner is intended only for systems the user owns or has explicit permission to scan.

## Target Users

- Students learning Java and object-oriented software design
- Developers performing basic local security checks
- Users who need lightweight local security auditing
- Academic evaluators reviewing Java concepts in a practical project

## High-Level Features

1. Concurrent TCP port scanning
2. SHA-256 file-integrity baseline creation
3. Detection of added, modified, and deleted files
4. Rule-based password-strength auditing
5. SQLite/JDBC persistence
6. Security-event history
7. Consolidated security report generation
8. Text report export using NIO.2
