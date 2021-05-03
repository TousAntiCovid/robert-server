-- NEED TO add the extension as superadmin before using uuid
-- as superadmin user: 
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE exposed_visits (
    id                VARCHAR(64) NOT NULL DEFAULT uuid_generate_v4(),
    ltid              UUID    NOT NULL,
    venue_type        INT     NOT NULL,
    venue_category1   INT     NOT NULL,
    venue_category2   INT     NOT NULL,
    period_start      BIGINT  NOT NULL,
    timeslot          INT     NOT NULL,
    backward_visits   BIGINT  NOT NULL DEFAULT 0,
    forward_visits    BIGINT  NOT NULL DEFAULT 0,
    qr_code_scan_time TIMESTAMP  WITH TIME ZONE DEFAULT NULL,

    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS exposed_visits_ltidperiodslots ON exposed_visits (ltid, period_start, timeslot);

