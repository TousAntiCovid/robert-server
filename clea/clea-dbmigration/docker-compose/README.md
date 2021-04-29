# DB Migration sample usage

[Flyway](https://flywaydb.org/) is a database migration tools that help to apply changes to different environments.

this docker-compose  simulate the current situation in INT,PPROD and PROD platform where
some scripts were already applied manually in database, and some will be proceeded by flyway.

**demo** script contains some useful shortcut
```bash
$ ./demo up    # launch in foreground the project (block the terminal)
$ ./demo up -d # launch in background the project
$ ./demo down  # stop the project and remove volumes, network

$./demo psql    # launch psql command line tool in postgres service
$./demo flyway  # launch flyway command line tool in flyway service
 
```

## Test scenario

V1 and v2 script are manually applied.
We wants to start to use flyway to execute V3 and others scripts.

### In first terminal:
```bash
$ ./demo up    # launch in foreground the project (block the terminal)
Creating network "demo_default" with the default driver
Creating demo_postgres_1 ... done
Creating demo_flyway_1   ... done
Attaching to demo_postgres_1, demo_flyway_1
postgres_1  | The files belonging to this database system will be owned by user "postgres".
postgres_1  | This user must also own the server process.
...
postgres_1  | /usr/local/bin/docker-entrypoint.sh: running /docker-entrypoint-initdb.d/00-init_privileges.sql
postgres_1  | You are now connected to database "cleadb" as user "postgres".
postgres_1  | REVOKE
postgres_1  | CREATE EXTENSION
postgres_1  | CREATE ROLE
postgres_1  | GRANT
postgres_1  | GRANT
...
postgres_1  | 2021-04-27 08:40:51.023 UTC [1] LOG:  listening on IPv4 address "0.0.0.0", port 5432
postgres_1  | 2021-04-27 08:40:51.023 UTC [1] LOG:  listening on IPv6 address "::", port 5432
postgres_1  | 2021-04-27 08:40:51.029 UTC [1] LOG:  listening on Unix socket "/var/run/postgresql/.s.PGSQL.5432"
postgres_1  | 2021-04-27 08:40:51.037 UTC [84] LOG:  database system was shut down at 2021-04-27 08:40:50 UTC
postgres_1  | 2021-04-27 08:40:51.043 UTC [1] LOG:  database system is ready to accept connections
```
### In second terminal:

Manually apply scripts V1 and V2:
(nota *"flyway/sql"* folder is mount in  *"/manual"* folder under postgres container)

```bash
$ ./demo psql -f /manual/V1__spring-batch-postgres.sql
CREATE TABLE
CREATE TABLE
CREATE TABLE
CREATE TABLE
CREATE TABLE
CREATE TABLE
CREATE SEQUENCE
CREATE SEQUENCE
CREATE SEQUENCE
$
$ ./demo psql -f /manual/V2__initdb.sql 
CREATE TABLE
CREATE INDEX
$
```

if we then execute flyway, we have some errors:

```bash
$ ./demo flyway validate
Flyway Community Edition 7.8.1 by Redgate
Database: jdbc:postgresql://postgres/cleadb (PostgreSQL 13.2)
ERROR: Validate failed: Migrations have failed validation
Detected resolved migration not applied to database: 1. To fix this error, either run migrate, or set -ignorePendingMigrations=true.
Detected resolved migration not applied to database: 2. To fix this error, either run migrate, or set -ignorePendingMigrations=true.
Detected resolved migration not applied to database: 3. To fix this error, either run migrate, or set -ignorePendingMigrations=true.
Detected resolved migration not applied to database: 4. To fix this error, either run migrate, or set -ignorePendingMigrations=true.
$

$ ./demo flyway migrate
Flyway Community Edition 7.8.1 by Redgate
Database: jdbc:postgresql://postgres/cleadb (PostgreSQL 13.2)
Successfully validated 4 migrations (execution time 00:00.014s)
ERROR: Found non-empty schema(s) "public" but no schema history table. Use baseline() or set baselineOnMigrate to true to initialize the schema history table.
$
```

Flyway has to be informed that scripts V1 and V2 are already installed, and to start to execute V3 scripts.
```bash
$ ./demo flyway migrate -baselineVersion=2 -baselineOnMigrate=true
Flyway Community Edition 7.8.1 by Redgate
Database: jdbc:postgresql://postgres/cleadb (PostgreSQL 13.2)
Successfully validated 4 migrations (execution time 00:00.017s)
Creating Schema History table "public"."flyway_schema_history" with baseline ...
1 rows affected
Successfully baselined schema with version: 2
Current version of schema "public": 2
Migrating schema "public" to version "3 - cluster periods stat location"
Migrating schema "public" to version "4 - baseline"
Successfully applied 2 migrations to schema "public", now at version v4 (execution time 00:00.059s)
```

Till now, flyway can be used to apply new DDL scripts:

```bash
$ ./demo flyway info
Flyway Community Edition 7.8.1 by Redgate
Database: jdbc:postgresql://postgres/cleadb (PostgreSQL 13.2)
Schema version: 4

+-----------+---------+-------------------------------+----------+---------------------+----------------+
| Category  | Version | Description                   | Type     | Installed On        | State          |
+-----------+---------+-------------------------------+----------+---------------------+----------------+
| Versioned | 1       | spring-batch-postgres         | SQL      |                     | Below Baseline |
|           | 2       | << Flyway Baseline >>         | BASELINE | 2021-04-27 08:52:05 | Baseline       |
| Versioned | 3       | cluster periods stat location | SQL      | 2021-04-27 08:52:05 | Success        |
| Versioned | 4       | baseline                      | SQL      | 2021-04-27 08:52:05 | Success        |
+-----------+---------+-------------------------------+----------+---------------------+----------------+

$ ./demo flyway migrate 
Flyway Community Edition 7.8.1 by Redgate
Database: jdbc:postgresql://postgres/cleadb (PostgreSQL 13.2)
Successfully validated 4 migrations (execution time 00:00.019s)
Current version of schema "public": 4
Schema "public" is up to date. No migration necessary.
```

## verify

we can check objects created in postgres

```bash
$ ./demo psql
psql (13.2 (Debian 13.2-1.pgdg100+1))
Type "help" for help.

cleadb=# \d
                      List of relations
 Schema |             Name             |   Type   |  Owner   
--------+------------------------------+----------+----------
 public | batch_job_execution          | table    | postgres
 public | batch_job_execution_context  | table    | postgres
 public | batch_job_execution_params   | table    | postgres
 public | batch_job_execution_seq      | sequence | postgres
 public | batch_job_instance           | table    | postgres
 public | batch_job_seq                | sequence | postgres
 public | batch_step_execution         | table    | postgres
 public | batch_step_execution_context | table    | postgres
 public | batch_step_execution_seq     | sequence | postgres
 public | cluster_periods              | table    | postgres
 public | exposed_visits               | table    | postgres
 public | flyway_schema_history        | table    | postgres
 public | stat_location                | table    | postgres
(13 rows)

cleadb=# \q
$
````
V3 script create *cluster_periods* and *stat_location* tables.

```bash
$ ./demo psql --command='SELECT version,type,script from flyway_schema_history'
 version |   type   |                script                 
---------+----------+---------------------------------------
 2       | BASELINE | << Flyway Baseline >>
 3       | SQL      | V3__cluster_periods_stat_location.sql
 4       | SQL      | V4__baseline.sql
(3 rows)
````
V2 is set as baseline, scripts V1 and V2 are ignored (not managed).
V3 script and upper versions are managed.

You see a V4_baseline.sql It's an empty script because we hope to rebase flyway on v4, start to manage V5.
And we also don't have empty Version number that can cause confusion.

We could choose to rebase on V3, but we then loose in git which version were applied manually, which where apply/managed by flyway.

## cleanup the environment

```bash
$ ./demo down
```
