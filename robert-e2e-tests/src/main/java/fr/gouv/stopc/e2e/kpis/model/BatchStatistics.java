package fr.gouv.stopc.e2e.kpis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Document
public class BatchStatistics {

    @Id
    private String id;

    @Indexed
    private Instant jobStartInstant;

    private long usersAboveRiskThresholdButRetentionPeriodExpired;

}
