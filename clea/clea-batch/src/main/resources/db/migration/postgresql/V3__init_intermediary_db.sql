-- NEED TO add the extension as superadmin before using uuid
-- as superadmin user:
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE cluster_periods
(
    id                          VARCHAR(64) NOT NULL DEFAULT uuid_generate_v4(),
    ltid                        UUID        NOT NULL,
    venue_type                  INT         NOT NULL,
    venue_category1             INT         NOT NULL,
    venue_category2             INT         NOT NULL,
    period_start                BIGINT      NOT NULL,
    first_timeslot             INT         NOT NULL,
    last_timeslot              INT         NOT NULL,
    cluster_start               BIGINT      NOT NULL,
    cluster_duration_in_seconds INT         NOT NULL,
    risk_level                  REAL        NOT NULL,

    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS cluster_periods_ltid ON cluster_periods (ltid);
