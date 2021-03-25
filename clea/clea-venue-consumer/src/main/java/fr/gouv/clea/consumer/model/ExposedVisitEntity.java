package fr.gouv.clea.consumer.model;

import java.time.Instant;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "EXPOSED_VISITS")
public class ExposedVisitEntity {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private String locationTemporaryPublicId;
    private int venueType;
    private int venueCategory1;
    private int venueCategory2;
    private long periodStart;
    private int timeSlot;
    private long backwardVisits;
    private long forwardVisits;

    private Instant qrCodeScanTime; // for purge

    @CreationTimestamp
    private Instant createdAt; // for db ops/maintenance

    @UpdateTimestamp
    private Instant updatedAt; // for db ops/maintenance
}
