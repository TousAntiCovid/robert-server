# DB Migration project with Flyway

[Flyway](https://flywaydb.org/) is a database migration tools that help to apply changes to different environments.


flyway is available as command-line tool and as docker image.

to execute it with Gitlab-CI, it is better to use the docker image.


## First rebase baseline

CLEA database was initially created manually by applying thoses scripts:
- V1__spring-batch-postgres.sql
- V2__initdb.sql
- V3__cluster_periods_stat_location.sql

We don't want Flyway to execute those files anymore.

To change the current version (and only apply upper versions) we can execute this command once :

```bash
$ flyway migrate  -baselineOnMigrate=true - baselineVersion=4 
```

Once the command is executed, future migrations can be apply by launching :
```bash
$ flyway migrate
```

the next script to apply will be named : *V5__xxx.sql*

    Remarks:
    You see a V4_baseline.sql It's an empty script because we hope to rebase flyway on v4, start to manage 
    scripts in V5.
    And we also don't want empty Version number that can cause confusion.

    We could choose to rebase on V3, but we then loose in git which version were applied manually, and
    which where apply/managed by flyway.


## Working with versions

An option could be to align DDL scripts with application version:

    ex: V1_1__Dll-for-application-v1.1.sql

But as Clea project is using Flyway community edition, we can't use **undo** feature. Once a script is apply in INT environment, it can't be undo.

The simpler solution is to increment script version without reference to the application version and with no **undo** scripts to validate and maintain. 
