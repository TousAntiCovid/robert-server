package fr.gouv.clea.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/*
 *       Column       |           Type           |              Modifiers
 * -------------------+--------------------------+-------------------------------------
 *  id                | character varying(64)    | not null default uuid_generate_v4()
 *  ltid              | uuid                     | not null
 *  venue_type        | integer                  | not null
 *  venue_category1   | integer                  | not null
 *  venue_category2   | integer                  | not null
 *  period_start      | bigint                   | not null
 *  timeslot          | integer                  | not null
 *  backward_visits   | bigint                   | not null default 0
 *  forward_visits    | bigint                   | not null default 0
 *  qr_code_scan_time | timestamp with time zone |
 *  created_at        | timestamp with time zone | not null default now()
 *  updated_at        | timestamp with time zone | not null default now()
 * Indexes:
 *     "exposed_visits_pkey" PRIMARY KEY, btree (id)
 *     "exposed_visits_ltidslots" btree (ltid, period_start, timeslot)
 */

/**
 * Exposed_visits structure.
 * <p>
 * This class is used in this module as Simple POJO, managed with JDBC and not with JPA.
 * <p>
 * The same class exist in clea-ws-consumer (ExposedVisitEntity) and is managed with JPA.
 * All references to javax.persistence or hibernate are keep in comment but not removed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExposedVisit {

    private String id;

    private UUID locationTemporaryPublicId;

    private int venueType;

    private int venueCategory1;

    private int venueCategory2;

    private long periodStart;

    private int timeSlot;

    private long backwardVisits;

    private long forwardVisits;

    private Instant qrCodeScanTime; // for purge but not used anymore

    private Instant createdAt; // for db ops/maintenance: purge

    private Instant updatedAt; // for db ops/maintenance
}
