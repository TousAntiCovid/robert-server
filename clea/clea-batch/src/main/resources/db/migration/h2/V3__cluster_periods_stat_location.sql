-- NEED TO add the extension as superadmin before using uuid
-- as superadmin user:
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


-- Needs: Clea-venue-Consumer
ALTER TABLE exposed_visits DROP COLUMN IF EXISTS qr_code_scan_time;

-- Needs: Clea-Batch
CREATE TABLE cluster_periods
(
    id                          VARCHAR(64) NOT NULL DEFAULT random_uuid(),
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

-- Needs: Clea-Statistiques
CREATE TABLE stat_location
(
    period           TIMESTAMP(0) WITH TIME ZONE NOT NULL,
    venue_type       INT NOT NULL,
    venue_category1  INT NOT NULL,
    venue_category2  INT NOT NULL,
    backward_visits  BIGINT NOT NULL,
    forward_visits   BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS statloc_period ON stat_location(period);
CREATE INDEX IF NOT EXISTS statloc_venue  ON stat_location(venue_type, venue_category1, venue_category2);


 