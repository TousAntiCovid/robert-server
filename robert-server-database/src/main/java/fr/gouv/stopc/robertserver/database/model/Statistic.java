package fr.gouv.stopc.robertserver.database.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Document(value = "webserviceStatistics")
public class Statistic {

    @Id
    private Instant date;

    private int notifiedTotal;

    public void incrementNotifiedTotal() {
        this.notifiedTotal++;
    }
}
