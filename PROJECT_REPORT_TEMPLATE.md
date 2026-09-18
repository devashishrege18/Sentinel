# Project Report: Sentinel — Local Security Monitoring Toolkit

> Check your course page for the officially required report format/template.
> If one is provided, use it and port the content below into it. This is a
> generic structure covering what most rubrics ask for.

## 1. Title Page
- Project Title: Sentinel — Local Security Monitoring Toolkit
- Course: Programming in Java
- Your Name / Roll Number
- Date of Submission

## 2. Abstract
Two to three sentences: what the project is, what problem it addresses
(lightweight, no-dependency local security auditing), and what Java
techniques it demonstrates.

## 3. Objectives
- Build a multi-module CLI tool covering the full course syllabus in one
  coherent application rather than isolated exercises.
- Demonstrate OOP design (encapsulation, interfaces, polymorphism),
  custom exception hierarchies, NIO.2 file I/O, the Collections/Stream
  API, concurrency via `ExecutorService`, and JDBC persistence.

## 4. System Architecture
Describe the six packages (`scanner`, `integrity`, `auth`, `persistence`,
`report`, `cli`) and how they depend on each other. Include or redraw the
package diagram from the README's "Project structure" section.

Suggested architecture description:
- `cli.Sentinel` is the single entry point; it depends on all other
  packages but none of them depend on it (keeps business logic decoupled
  from the presentation/menu layer).
- `scanner`, `integrity`, and `auth` are independent domain modules with
  no dependencies on each other.
- `persistence` depends on the domain packages (to persist their model
  objects) but not on `cli`.
- `report` depends on `persistence` (to read history) and the domain
  model classes (to aggregate them).

## 5. Modules Implemented

### 5.1 Port Scanner (`scanner` package)
Explain: concurrent scanning design, why a fixed thread pool was chosen,
the custom exception hierarchy (`ScanException` → `InvalidPortRangeException`,
`HostUnreachableException`), and the `PortScanResult` Builder.

### 5.2 File Integrity Monitor (`integrity` package)
Explain: SHA-256 baselining, the flat-file baseline format, how
added/modified/deleted files are detected, and where concurrency is used
(hashing multiple files in parallel).

### 5.3 Password Auditor (`auth` package)
Explain: the Strategy pattern via `PasswordRule`, each of the four rules
implemented, and why plaintext passwords are never persisted.

### 5.4 Persistence Layer (`persistence` package)
Explain: why SQLite was chosen (file-based, zero server setup), the
Singleton `DatabaseManager`, the `Repository<T>` abstraction, and the
schema (three tables: `scan_results`, `integrity_events`,
`password_audits`).

### 5.5 Reporting (`report` package)
Explain: how `SecurityReportGenerator` uses the Stream API (`groupingBy`,
`counting`, `average`) to aggregate data from all three modules, and how
`ReportExporter` writes the result to disk using NIO.2.

## 6. Design Patterns Used
| Pattern | Where | Why |
|---|---|---|
| Singleton | `DatabaseManager`, `ConfigManager` | one shared connection/config instance |
| Builder | `PortScanResult.Builder` | many optional/derived fields |
| Strategy | `PasswordRule` implementations | interchangeable, independently-addable rules |
| DAO/Repository | `Repository<T>` and implementations | isolates SQL from domain/CLI code |

## 7. Course Concepts Mapped to Code
Reproduce the table from the README section "Course concepts covered" —
this is usually exactly what an automated rubric checks for.

## 8. Testing / Sample Runs
Include screenshots or terminal transcripts of:
- A port scan against `localhost` showing open ports found.
- Creating a baseline, editing a file, then running an integrity check
  and seeing the change detected.
- A password audit run against both a weak and a strong password.
- A generated report showing aggregated results.

## 9. Challenges Faced
Write this section yourself, in your own words, based on what actually
gave you trouble while building or understanding the project (e.g.
reasoning about thread pool sizing, handling SQLite connection
lifecycle, designing the exception hierarchy).

## 10. Conclusion
Summarize what the project demonstrates and, optionally, what you would
add given more time (e.g. scheduled/background monitoring, a config
file, more password rules, export to CSV/JSON).

## 11. References
- Oracle Java documentation (docs.oracle.com)
- SQLite JDBC driver (github.com/xerial/sqlite-jdbc)
- Any course materials/slides referenced
