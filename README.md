# Sentinel — Local Security Monitoring Toolkit

Sentinel is a command-line Java application that bundles three security
utilities behind one menu:

1. **Port Scanner** — concurrently probes a range of TCP ports on a target
   host and reports which are open, closed, or filtered.
2. **File Integrity Monitor** — hashes every file in a chosen directory
   (SHA-256), stores a baseline, and later detects added, modified, or
   deleted files by comparing against that baseline.
3. **Password Auditor** — checks a password against a configurable set of
   strength rules (length, character complexity, common-password list,
   sequential/repeated-character patterns).

All results are persisted to a local SQLite database file, and a combined
summary report across all three tools can be generated and exported to a
text file at any time.

> **Intended use:** the port scanner is meant to be run against hosts you
> own or have explicit permission to scan (e.g. `localhost` or your own
> machine on a LAN). Scanning third-party systems without authorization is
> illegal in most jurisdictions.

---

## 1. Prerequisites

You need two things installed: a **Java Development Kit (JDK), version 17
or later**, and **Apache Maven**. Nothing else — the project uses SQLite,
which is file-based and requires no separate database server to install
or run.

### 1.1 Installing a JDK (version 17+)

**Windows:**
1. Download the installer from [Eclipse Temurin](https://adoptium.net/) (choose JDK 17 or later, Windows x64 `.msi`).
2. Run the installer, keeping the default options (make sure "Add to PATH" is checked).
3. Open a new Command Prompt and confirm with:
   ```
   java -version
   ```

**macOS:**
```
brew install openjdk@17
```
Then confirm with `java -version`. If Homebrew is not installed, get it from [brew.sh](https://brew.sh/) first.

**Linux (Debian/Ubuntu):**
```
sudo apt update
sudo apt install openjdk-17-jdk
java -version
```

### 1.2 Installing Apache Maven

**Windows:**
1. Download the binary zip from [Maven's download page](https://maven.apache.org/download.cgi).
2. Extract it to e.g. `C:\Program Files\Maven`.
3. Add `C:\Program Files\Maven\bin` to your system `PATH` environment variable.
4. Open a new Command Prompt and confirm with:
   ```
   mvn -version
   ```

**macOS:**
```
brew install maven
mvn -version
```

**Linux (Debian/Ubuntu):**
```
sudo apt install maven
mvn -version
```

---

## 2. Getting the project

```
git clone https://github.com/<github-username>/<repo-name>.git
cd <repo-name>
```

---

## 3. Building the project

From the project root (the folder containing `pom.xml`):

```
mvn clean package
```

This downloads the one required dependency (the SQLite JDBC driver) and
compiles the project. On success, a runnable jar is created at
`target/sentinel.jar`.

---

## 4. Running the project

You can run it either directly with Maven, or as the packaged jar.

**Option A — via Maven (no packaging needed):**
```
mvn exec:java
```

**Option B — as a standalone jar (after `mvn clean package`):**
```
java -jar target/sentinel.jar
```

Either way, you'll see a menu:

```
============================================================
  SENTINEL - Local Security Monitoring Toolkit
============================================================

1) Scan ports on a host
2) Create/refresh a file integrity baseline
3) Check a directory against its baseline
4) Audit a password's strength
5) Generate and export a security report
6) Exit
Choose an option:
```

A file named `sentinel.db` (SQLite database) is created automatically in
the project root the first time you run the application. No setup is
required for this — it is just a file.

---

## 5. Using each feature

### Option 1 — Scan ports on a host
Enter a host (e.g. `localhost`) and a port range (e.g. start `1`, end
`1024`). Sentinel scans every port in that range concurrently and prints
the ones found open, along with response latency. Results are saved to
the database.

### Option 2 — Create/refresh a file integrity baseline
Enter a directory path (e.g. a folder with a few test files in it).
Sentinel hashes every file inside it and stores the baseline in a hidden
file, `.sentinel-baseline.txt`, inside that same directory.

### Option 3 — Check a directory against its baseline
Point Sentinel at a directory that already has a baseline (Option 2).
Try editing, adding, or deleting a file in that folder, then run this
option — Sentinel will report exactly what changed and log it to the
database.

### Option 4 — Audit a password's strength
Type in a candidate password. Sentinel checks it against four rules:
minimum length, character-class complexity, presence in a common-password
list, and simple sequential/repeated patterns (e.g. `abcd`, `1111`). Only
the pass/fail summary is stored — the password text itself is never
written to disk or the database.

### Option 5 — Generate and export a security report
Produces a combined summary of everything recorded so far (scans,
integrity events, password audits) and optionally writes it to a
timestamped `.txt` file under a local `reports/` folder.

---

## 6. Project structure

```
sentinel/
├── pom.xml
├── README.md
└── src/main/java/com/sentinel/
    ├── scanner/       Port scanning: PortScanner, PortScanResult, PortStatus,
    │                  and the ScanException hierarchy
    ├── integrity/      File integrity monitoring: FileIntegrityMonitor,
    │                  FileRecord, ChangeEvent, ChangeType, IntegrityException
    ├── auth/           Password auditing: PasswordAuditor and pluggable
    │                  PasswordRule strategies (Length, Complexity, Common,
    │                  Sequential)
    ├── persistence/    SQLite/JDBC layer: DatabaseManager (Singleton),
    │                  Repository<T> interface, and per-entity repositories
    ├── report/         SecurityReportGenerator (Stream-based aggregation)
    │                  and ReportExporter (NIO.2 file writing)
    └── cli/            Sentinel (main entry point / menu loop) and
                       ConfigManager (Singleton)
```

## 7. Design patterns used

- **Singleton** — `DatabaseManager` (one shared SQLite connection) and
  `ConfigManager` (one shared configuration instance).
- **Builder** — `PortScanResult.Builder`, used because scan results carry
  several optional/derived fields that read poorly as a long constructor.
- **Strategy** — `PasswordRule` implementations are interchangeable
  strategies injected into `PasswordAuditor`; new rules can be added
  without modifying the auditor.
- **DAO / Repository** — `Repository<T>` and its implementations isolate
  all SQL from the domain and CLI layers.

## 8. Course concepts covered

| Module | Where |
|---|---|
| OOP (encapsulation, interfaces, polymorphism) | `scanner`, `auth` packages |
| Custom exceptions | `ScanException` hierarchy, `IntegrityException` |
| File I/O with NIO.2 | `FileIntegrityMonitor`, `ReportExporter` |
| Date/Time API | `PortScanResult`, `ChangeEvent`, `PasswordAuditResult` |
| Collections & generics | `Repository<T>`, `Map`/`List` usage throughout |
| Stream API | `SecurityReportGenerator`, `PasswordAuditResult` |
| Concurrency (`ExecutorService`) | `PortScanner`, `FileIntegrityMonitor` |
| JDBC | `persistence` package (SQLite) |

---

## 9. Notes

- The SQLite database file (`sentinel.db`) and any exported reports
  (`reports/`) are created next to wherever you run the jar from; they are
  git-ignored so they won't be committed.
- No external services, API keys, or accounts are required to build or
  run this project.
