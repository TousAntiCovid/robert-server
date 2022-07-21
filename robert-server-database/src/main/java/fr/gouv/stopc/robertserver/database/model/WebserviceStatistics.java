package fr.gouv.stopc.robertserver.database.model;

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
@Builder(toBuilder = true)
@Document
public class WebserviceStatistics {

    @Id
    private String id;

    @Indexed
    private Instant date;

    private Long totalAlertedUsers;

    private Long totalExposedButNotAtRiskUsers;

    private Long totalInfectedUsersNotNotified;

    private Long totalNotifiedUsersScoredAgain;

    private Long notifiedUsers;

    private Long reportsCount;
}
