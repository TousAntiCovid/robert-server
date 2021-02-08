package fr.gouv.tacw.database.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@Entity
@Table(name = "EXPOSED_STATIC_VISIT", indexes = { @Index(columnList = "token") })
public class ExposedStaticVisitEntity {
    @Id
    @GeneratedValue
    @ToString.Exclude
    private UUID id;

    @NotNull
    private byte[] token;
    
    @Enumerated
    @Column(columnDefinition = "smallint")
    private RiskLevel venueRiskLevel;

    @NotNull
    private long visitStartTime;

    @NotNull
    private long visitEndTime;

    /*
     * Delta to remove from the visitTime of Exposure Status Request opaque visits
     * to get the opaque visit start time to check against this exposed visit.
     */
    @NotNull
    private long startDelta;

    /*
     * Delta to add to the visitTime of Exposure Status Request opaque visits to get
     * the opaque visit end time to check against this exposed visit.
     */
    @NotNull
    private long endDelta;

    @NotNull
    private long exposureCount;

    public ExposedStaticVisitEntity(@NotNull byte[] token, @NotNull RiskLevel venueRiskLevel, 
            @NotNull long visitStartTime, @NotNull long visitEndTime,
            @NotNull int startDelta, @NotNull int endDelta, @NotNull long exposureCount) {
        super();
        this.token = token;
        this.venueRiskLevel = venueRiskLevel;
        this.visitStartTime = visitStartTime;
        this.visitEndTime = visitEndTime;
        this.startDelta = startDelta;
        this.endDelta = endDelta;
        this.exposureCount = exposureCount;
    }
}
