# TITAN PostgreSQL Backup, Restore and Persistence Runbook

## Purpose and scope

This runbook documents the tested local backup and restore procedure for TITAN's PostgreSQL 17 database:

- Database: `titan_commerce`
- Database user: `titan_user`
- Compose service: `postgres`
- External Docker volume: `titan-postgres-data`
- PostgreSQL data directory: `/var/lib/postgresql/data`

The procedures use PowerShell from the repository root and the existing Docker Compose stack. They distinguish container recreation (which reuses the volume) from logical backup/restore (which creates an independent archive).

This is a local backup/restore procedure. It is not an off-host backup strategy, point-in-time recovery system, or production disaster-recovery plan.

## 1. Protect backup artifacts from Git

The repository `.gitignore` excludes the local `backups/` directory:

```gitignore
# Local database backups
/backups/
```

Create the directory on the Windows host:

```powershell
New-Item -ItemType Directory -Force -Path .\backups
```

Confirm a backup file is ignored:

```powershell
git check-ignore -v .\backups\titan_commerce_YYYY-MM-DD.dump
```

Ignoring a file prevents accidental Git tracking; it does not encrypt the archive or protect it from other local users, malware, accidental deletion, or disk failure. Treat database backups as sensitive files. Do not commit them, attach them to public issues, or upload them to an unapproved service.

## 2. Confirm the persistent volume and database baseline

Inspect the Docker volume:

```powershell
docker volume inspect titan-postgres-data
```

Confirm the running PostgreSQL container mounts the volume at the expected data directory:

```powershell
docker inspect titan-commerce-postgres-1 --format '{{range .Mounts}}{{.Name}} -> {{.Destination}}{{end}}'
```

Expected mapping:

```text
titan-postgres-data -> /var/lib/postgresql/data
```

List tables:

```powershell
docker compose exec -T postgres psql -U titan_user -d titan_commerce -c "\dt"
```

Record representative row counts before taking a backup:

```powershell
docker compose exec -T postgres psql -U titan_user -d titan_commerce -c "SELECT 'products' AS table_name, COUNT(*) AS row_count FROM products UNION ALL SELECT 'customers', COUNT(*) FROM customers;"
```

The M2-06 test baseline was:
- `products`: 5 rows
- `customers`: 0 rows

These counts describe the local database at the time of this test, not an invariant for future application data.

## 3. Create a custom-format logical backup

The tested procedure writes the dump inside the PostgreSQL container first, then copies it to the Windows host. This avoids shell redirection of binary archive data.

Example filename for the test date:

```powershell
docker compose exec -T postgres pg_dump -U titan_user -d titan_commerce -Fc -f /tmp/titan_commerce_YYYY-MM-DD.dump
```

Option meanings:
- `pg_dump` creates a logical backup of one database.
- `-U titan_user` selects the database user.
- `-d titan_commerce` selects the source database.
- `-Fc` writes PostgreSQL's custom archive format, suitable for `pg_restore`.
- `-f` selects the output file inside the container.

Copy the archive to the host, replacing the date with the filename actually used:

```powershell
docker cp titan-commerce-postgres-1:/tmp/titan_commerce_YYYY-MM-DD.dump .\backups\titan_commerce_YYYY-MM-DD.dump
```

Inspect its size and timestamp:

```powershell
Get-Item .\backups\titan_commerce_YYYY-MM-DD.dump | Format-List Name, Length, LastWriteTime
```

Validate that PostgreSQL can read the archive's table of contents:

```powershell
docker compose exec -T postgres pg_restore --list /tmp/titan_commerce_YYYY-MM-DD.dump
```

Optionally validate the host copy by copying it back into the container under a separate temporary filename and listing that archive:

```powershell
docker cp .\backups\titan_commerce_YYYY-MM-DD.dump titan-commerce-postgres-1:/tmp/titan_commerce_restore_test.dump
docker compose exec -T postgres pg_restore --list /tmp/titan_commerce_restore_test.dump
```

A successful `pg_restore --list` confirms the archive is readable and shows the objects recorded in it. It does not prove a restore will succeed; a restore into a separate test database is required for that.

### M2-06 backup evidence

The archive created during the test was:
- File: `backups/titan_commerce_2026-11-10.dump`
- Size on Windows host: 4,717 bytes
- Format: PostgreSQL custom archive (`CUSTOM`), gzip compression
- Source/dump version: PostgreSQL 17.11
- Table of contents included the `products` and `customers` tables, their data, sequences, and constraints.
- The host backup file was confirmed ignored by `git check-ignore -v` using the `/backups/` rule.

The backup file remains local and is intentionally not stored in the Git repository.

## 4. Restore into an isolated test database

**Never test restoration by overwriting the working `titan_commerce` database.** Create a separate, empty target database.

The test target used during M2-06 was `titan_commerce_restore_test_20261110`.

Create it:

```powershell
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U titan_user -d postgres -c "CREATE DATABASE titan_commerce_restore_test_YYYYMMDD;"
```

Restore the archive into that target (use the exact target name created above and the archive path copied into the container):

```powershell
docker compose exec -T postgres pg_restore --exit-on-error --no-owner --no-privileges -U titan_user -d titan_commerce_restore_test_YYYYMMDD /tmp/titan_commerce_restore_test.dump
```

Option meanings:
- `--exit-on-error` stops the restore when an error occurs.
- `--no-owner` does not attempt to apply the original object-owner assignments.
- `--no-privileges` does not apply archived GRANT/REVOKE statements.
- `-U titan_user` selects the restoring database user.
- `-d` selects the already-created target database.

Do not add `--clean` or `--create` for this procedure. The target should be a newly created empty database.

**Run the restore once per empty target.** A successful `pg_restore` normally produces no output. Running it again against the same populated target attempts to create tables that already exist and produces errors such as `relation "customers" already exists`. That duplicate-object error does not establish that the first restore failed. Do not repeatedly retry against a populated target. For another test, create a new uniquely named empty test database.

## 5. Validate restored schema and data

List restored tables:

```powershell
docker compose exec -T postgres psql -U titan_user -d titan_commerce_restore_test_YYYYMMDD -c "\dt"
```

Inspect the product table definition:

```powershell
docker compose exec -T postgres psql -U titan_user -d titan_commerce_restore_test_YYYYMMDD -c "\d products"
```

Compare representative row counts:

```powershell
docker compose exec -T postgres psql -U titan_user -d titan_commerce_restore_test_YYYYMMDD -c "SELECT 'products' AS table_name, COUNT(*) AS row_count FROM products UNION ALL SELECT 'customers', COUNT(*) FROM customers;"
```

Also inspect representative rows where appropriate:

```powershell
docker compose exec -T postgres psql -U titan_user -d titan_commerce_restore_test_YYYYMMDD -c "SELECT * FROM products ORDER BY id LIMIT 3;"
```

During M2-06:
- The restored test database contained 5 products and 0 customers, matching the source baseline.
- The restored table list, product definition, and sample rows were inspected.
- The first restore completed without reported errors. A subsequent duplicate attempt returned `relation "customers" already exists`, consistent with attempting to restore again into the already-populated target.

A matching row count is a useful validation but not a full data-integrity proof for every row. For higher assurance, compare representative values or use application-level tests appropriate to the data.

## 6. Prove persistence across PostgreSQL container recreation

The PostgreSQL data directory is stored in the external named volume `titan-postgres-data`. Recreating the container should reattach the same volume.

Before recreation, record row counts in the original database and confirm that the volume is mounted at `/var/lib/postgresql/data`.

Recreate only the PostgreSQL service:

```powershell
docker compose up -d --force-recreate postgres
```

This replaces the service container and reuses its configured volume. It is not a database restore and does not remove the volume.

Wait for PostgreSQL to accept connections:

```powershell
docker compose exec -T postgres pg_isready -U titan_user -d titan_commerce
```

Re-run the original database row-count query from Section 2. During M2-06, PostgreSQL accepted connections after recreation and the original database still contained 5 products and 0 customers. The named volume remained mounted at the expected data directory.

## 7. Data safety and recovery considerations

The Compose file declares the volume as external:

```yaml
volumes:
  postgres_data:
    external: true
    name: titan-postgres-data
```

**Never remove `titan-postgres-data` or use `docker compose down -v` as a routine troubleshooting or recovery action.** Do not use indiscriminate `docker volume prune` or manually delete the PostgreSQL data directory. Volume deletion can destroy the only copy of the working database.

A normal `docker compose down` does not remove this external volume, but it does stop and remove Compose containers and the Compose network. For a PostgreSQL container-only recreation, prefer the targeted command in Section 6.

A logical dump is independent of the live database volume, but the tested archive was copied to the same host running Docker. It therefore protects against some container-level failures, not host disk failure, host compromise, or loss of the machine. Maintain an additional protected off-host copy before treating important data as recoverable.

The custom-format archive may contain customer records and sensitive application data. Store it securely, restrict access, and apply encryption and retention controls when moving it beyond the local development machine. The `.gitignore` rule is not a substitute for secure backup storage.

## 8. Tested results and limitations

| Test | M2-06 result |
|---|---|
| Named volume exists and is mounted at PostgreSQL data directory | Passed |
| Source tables and initial row counts recorded | Passed: 2 tables, 5 products, 0 customers |
| Custom-format logical backup created | Passed |
| Archive readable by `pg_restore --list` | Passed |
| Host backup file exists and is ignored by Git | Passed |
| Backup copied back into container and archive read | Passed |
| Restore into separate test database | Passed |
| Restored schema and representative product data inspected | Passed |
| Restored row counts match source baseline | Passed |
| PostgreSQL container recreated with same named volume | Passed |
| Original data remains after container recreation | Passed |

Limitations:
- This was a manual local backup and restore exercise, not a scheduled or automated backup system.
- The host backup was not copied off the machine and no remote retention was configured.
- No point-in-time recovery or WAL archiving was configured.
- No host-loss or disaster-recovery scenario was tested.
- The observed counts are the test baseline, not permanent expected production counts.

Before using TITAN with important or production data, add automated backup scheduling, protected off-host storage, retention policy, integrity monitoring, and periodic restore drills.
