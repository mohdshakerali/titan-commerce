# PostgreSQL Connection Failure — Invalid TimeZone

## Issue Details

| Field | Details |
|---|---|
| Component | Spring Boot / PostgreSQL |
| Database | PostgreSQL 17 |
| Java | Java 21 |
| Environment | Windows 11 + Docker Desktop |
| Error | Invalid TimeZone: Asia/Calcutta |
| Status | Resolved |

## 1. Problem Description

During Spring Boot integration testing, the application failed to establish a connection with the PostgreSQL database.

Hibernate also reported that it could not determine the database dialect because the JDBC connection could not be established.

The primary error was:

```text
org.postgresql.util.PSQLException:
FATAL: invalid value for parameter "TimeZone": "Asia/Calcutta"
```

## 2. Root Cause

The Java process was using the timezone identifier:

```text
Asia/Calcutta
```

PostgreSQL 17 rejected this timezone identifier during connection initialization.

The canonical IANA timezone identifier for India is:

```text
Asia/Kolkata
```

This was a Java-to-PostgreSQL timezone compatibility issue, not a database availability, authentication or network connectivity problem.

## 3. Troubleshooting and Resolution

We configured the Java timezone through the PowerShell environment.

Command:

```powershell
$env:JAVA_TOOL_OPTIONS = "-Duser.timezone=Asia/Kolkata"
```

This sets a JVM startup option for Java processes launched from the current PowerShell session.

The setting does not permanently modify the Windows system timezone.

## 4. Verification

First, we reran the individual product persistence test:

```powershell
.\mvnw.cmd -Dtest=TitanBackendApplicationTests#shouldSaveAndRetrieveProduct test
```

Result:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

We then executed the complete test suite:

```powershell
.\mvnw.cmd clean test
```

Result:

```text
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 5. Lessons Learned

- A Hibernate dialect error does not always mean the dialect configuration is incorrect.
- Inspect the first underlying exception before changing application or database configuration.
- PostgreSQL connection initialization can fail because of client-supplied connection parameters.
- JVM timezone configuration can be controlled through startup options.
- Environment-specific issues should be documented for repeatability.

## 6. Final Status

**Resolved.**

The PostgreSQL connection was successfully established, and all seven integration tests passed.

No Windows system timezone change was required.

## 7. Future Considerations

For consistent development environments, document and standardize JVM timezone configuration.

For production deployments, configure JVM options through the deployment environment rather than relying on an interactive PowerShell session.
