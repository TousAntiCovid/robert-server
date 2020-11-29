# TAC-Warning: database volume estimate

**Warning: this is a back-of-the-envelope calculation. Is is meant to be somewhat pessimistic.**

## Goal

Estimate the steady-state size of the TAC-warning database.

It is expected that the database size will be dominated by the visit tokens table.

## Estimate

We want to estimate the number of new visit tokens stored on each day, and
accumulate it to estimate steady-state size.

Tokens older than RETENTION_DAYS are deleted by a periodic task.

(RETENTION_DAYS is identical on the client and the server -- each client reports its last RETENTION_DAYS worth of visits)

On a given day, NB_DAILY_COVID_REPORTS users (who have been tested positive to Covid-19) will report their visits through the app.

For each Covid-19-positive person, we estimate an average number of visited places of
(less than) VISITS_PER_DAY.

For each visit, a number of visit tokens are generated. This number can be expressed as

```
TOKENS_PER_VISIT = SALT_RANGE 
```

```
TOKENS_PER_REPORT_PER_DAY = TOKENS_PER_VISIT * VISITS_PER_DAY
```

Therefore, the number of new tokens per day is

```
NB_NEW_TOKENS_PER_DAY = NB_DAILY_COVID_REPORTS * TOKENS_PER_REPORT_PER_DAY
```

The size of a single token is constant (TOKEN_SIZE, expected to be 32 bytes)

The total steady-state size of the database table is given by

```
STEADY_STATE_SIZE = [NB_NEW_TOKENS_PER_DAY + (NB_NEW_TOKENS_PER_DAY*RETENTION_DAYS)]/2 * RETENTION_DAYS * TOKEN_SIZE 
```

(sum of an arithmetic series)

(**Note that RETENTION_DAYS appears twice in the formula**)

## Example 

N_DAILY_COVID_REPORTS = 5000 (extrapolated from a maximum of 2000 daily reports
and the target user base of 20 million users)

RETENTION_DAYS = 14 

VISITS_PER_DAY = 5

SALT_RANGE = 1000 (current proposal)

Yields the following values:

TOKENS_PER_VISIT = 1000 

TOKENS_PER_REPORT_PER_DAY = 1000 * 5 = 5000

NB_NEW_TOKENS_PER_DAY = 5000 * 5000 = 25000000

STEADY_STATE_TOKENS = [25000000 + (25000000*14)] / 2 * 14 = 2625000000

STEADY_STATE_SIZE = 2625000000 * 32 =  (84000000000 bytes -- or 84 gigabytes)
