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

On a given day, N_DAILY_COVID_REPORTS users (who have been tested positive to COVID) will report their visits through the app.

For each COVID-positive person, we estimate an average number of visited places of
(less than) VISITS_PER_DAY.

For each visit, a number of visit tokens are generated. This number can be expressed as

```
TOKENS_PER_VISIT = SALT_RANGE * (VISIT_DURATION / TIME_ATOM)
```

(VISIT_DURATION is a fixed value for each venue type, it does not depend on the actual visit duration -- in this estimate we use a single value for VISIT_DURATION)


```
TOKENS_PER_REPORT_PER_DAY = TOKENS_PER_VISIT * VISITS_PER_DAY
```

Therefore, the number of new tokens per day is

```
NEW_TOKENS_PER_DAY = N_DAILY_COVID_REPORTS * TOKENS_PER_REPORT_PER_DAY
```

The size of a single token is constant (TOKEN_SIZE, expected to be 32 bytes)

The total steady-state size of the database table is given by

```
STEADY_STATE_SIZE = [NEW_TOKENS_PER_DAY + (NEW_TOKENS_PER_DAY*RETENTION_DAYS)]/2 * RETENTION_DAYS * TOKEN_SIZE 
```

(sum of an arithmetic series)

(**Note that RETENTION_DAYS appears twice in the formula**)

## Example 

N_DAILY_COVID_REPORTS = 5000 (extrapolated from a maximum of 2000 daily reports
and the target user base of 20 million users)

RETENTION_DAYS = 14 

VISITS_PER_DAY = 5

SALT_RANGE = 1000 (current proposal)

VISIT_DURATION = 180 (minutes)

TIME_ATOM = 60 (minutes)

Yields the following values:

TOKENS_PER_VISIT = 1000 * (180 / 60) = 3000

TOKENS_PER_REPORT_PER_DAY = 3000 * 5 = 15000

NEW_TOKENS_PER_DAY = 5000 * 15000 = 75000000

STEADY_STATE_TOKENS = [75000000 + (75000000*14)] / 2 * 14 = 7875000000 

STEADY_STATE_SIZE = 7875000000 * 32 = 252000000000 (bytes -- or 252 gigabytes)

## Note

This assumes a scheme where discrete tokens are used (with a TIME_ATOM resolution). If this estimated 
volume is impractical, another scheme could be used where tokens would be interval-based. This would result
in a space savings factor of VISIT_DURATION/TIME_ATOM (e.g. a factor of 6 on the above example).
