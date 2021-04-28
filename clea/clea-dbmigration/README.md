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

# How to: Gitlab-ci


## Flyway template job:

This template is conform to other templates in current *backend-server* *gitlab-ci.yml* file.

```yaml
.flyway_template:
  image: 
    name: flyway/flyway:7.8
    entrypoint: [""]
  stage: deploy
  script:
    - cd $COMPONENT/$SUBCOMPONENT
    - flyway info
    - flyway migrate
    - flyway info
  only:
    - develop
    - tags
  tags:
    - ci
```

It use *COMPONENT* and *SUBCOMPONENT* variables to move to **clea-dbmigration** folder, the template is not specific to *clea* and can be reuse in other component.

All jobs based on this template will be execute at **deploy** phase of the pipeline

*Flyway* image has the *flyway* executable as entrypoint. it is a problem because gitlab launch an image with *sh* command, then execute defined script lines from STDIN. So, the template remove (empty) the default entrypoint.

This template execute 3 times flyway:
- the first time to see the configuration before migration
- the second apply the migration process
- the last display the configuration after the migration.

Only *flyway migrate* is mandatory.
To take in account that 3 DDL scripts where manually applied, the migration process line should be:

``̀
    - flyway migrate -baselineOnMigrate=true -baselineVersion=4
``̀

The **only** clause and **tags** clause should be adapt to the deployment pipelines and my be move from the template to the real job.

In this configuration, flyway will find *./conf/flyway.conf* file and "./sql/*.sql" ddl scripts.


## Deployment Job

The project define that flyway should be "manually" executed at "deploy" step.

```yaml
deploy-clea-migration:
  extends: .flyway_template
  #needs: ["test_clea"]
  variables:
    COMPONENT: "clea"
    SUBCOMPONENT: "clea-dbmigration"
    FLYWAY_URL: "$CLEA_DB_URL"
    FLYWAY_USER: "$CLEA_DBADM_USER"
    FLYWAY_PASSWORD: "$CLEA_DBADM_PASSWORD"
  when: manual
```

I's not sure that the job needs "test_clea". it is possible that test fails because latest changes in applications need an upgrade of the DBMS schema.

An important part is the definition of **FLYWAY_*** variables.

Thoses variables should not be defined in Gilab-ci CI/CD Settings because different components can use different databases.

In this situation, 3 **CLEA_DB*** protected (and masked) variables are define at project level and used in the job to build **FLYWAY** variables.

Here, the job is defined as manual task.

## Sample execution

Her is a log for this job. 

(that apply all ddl because we don't use **-baselineOnMigrate=true -baselineVersion=4** arguments )
```
Running with gitlab-runner 13.11.0 (7f7a4bb0)
  on runer-stable GsbJXce_
Preparing the "docker" executor 00:03
Using Docker executor with image flyway/flyway:7.8 ...
Pulling docker image flyway/flyway:7.8 ...
Using docker image sha256:b29707070fb62dea14ce3a037fd02bc1b9beef65895baf2cfbc59fac135ef17e for flyway/flyway:7.8 with digest flyway/flyway@sha256:bbe3751de5ec077160ff8eef5529fe7e109bf297d71bb24b3ec79c73d5e666c1 ...
Preparing environment
Running on runner-gsbjxce-project-3-concurrent-0 via 0295ca052308...
Getting source from Git repository 00:01
Fetching changes with git depth set to 50...
Reinitialized existing Git repository in /builds/stopcovid19/local-clea/.git/
Checking out f5e36b87 as develop...
Updating/initializing submodules recursively with git depth set to 50...
Restoring cache 00:02
Checking cache for default...
No URL provided, cache will not be downloaded from shared cache server. Instead a local version of cache will be extracted. 
Successfully extracted cache
Executing "step_script" stage of the job script 00:02
Using docker image sha256:b29707070fb62dea14ce3a037fd02bc1b9beef65895baf2cfbc59fac135ef17e for flyway/flyway:7.8 with digest flyway/flyway@sha256:bbe3751de5ec077160ff8eef5529fe7e109bf297d71bb24b3ec79c73d5e666c1 ...
$ cd $COMPONENT/$SUBCOMPONENT
$ flyway info
Flyway Community Edition 7.8.2 by Redgate
Database: [MASKED] (PostgreSQL 13.2)
Schema version: << Empty Schema >>
+-----------+---------+-------------------------------+------+--------------+---------+
| Category  | Version | Description                   | Type | Installed On | State   |
+-----------+---------+-------------------------------+------+--------------+---------+
| Versioned | 1       | spring-batch-[MASKED]         | SQL  |              | Pending |
| Versioned | 2       | initdb                        | SQL  |              | Pending |
| Versioned | 3       | cluster periods stat location | SQL  |              | Pending |
| Versioned | 4       | baseline                      | SQL  |              | Pending |
+-----------+---------+-------------------------------+------+--------------+---------+
$ flyway migrate
Flyway Community Edition 7.8.2 by Redgate
Database: [MASKED] (PostgreSQL 13.2)
Successfully validated 4 migrations (execution time 00:00.014s)
Creating Schema History table "public"."flyway_schema_history" ...
Current version of schema "public": << Empty Schema >>
Migrating schema "public" to version "1 - spring-batch-[MASKED]"
Migrating schema "public" to version "2 - initdb"
Migrating schema "public" to version "3 - cluster periods stat location"
Migrating schema "public" to version "4 - baseline"
Successfully applied 4 migrations to schema "public", now at version v4 (execution time 00:00.127s)
$ flyway info
Flyway Community Edition 7.8.2 by Redgate
Database: [MASKED] (PostgreSQL 13.2)
Schema version: 4
+-----------+---------+-------------------------------+------+---------------------+---------+
| Category  | Version | Description                   | Type | Installed On        | State   |
+-----------+---------+-------------------------------+------+---------------------+---------+
| Versioned | 1       | spring-batch-[MASKED]         | SQL  | 2021-04-27 15:22:07 | Success |
| Versioned | 2       | initdb                        | SQL  | 2021-04-27 15:22:07 | Success |
| Versioned | 3       | cluster periods stat location | SQL  | 2021-04-27 15:22:07 | Success |
| Versioned | 4       | baseline                      | SQL  | 2021-04-27 15:22:07 | Success |
+-----------+---------+-------------------------------+------+---------------------+---------+
Saving cache for successful job 00:01
Creating cache default...
WARNING: .m2/repository/: no matching files        
Archive is up to date!                             
Created cache
Job succeeded
```

