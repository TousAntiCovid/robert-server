package fr.gouv.stopc.robertserver.database.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Document
public class WebserviceStatistic {

    @Id
    private Instant date;

    private int notifiedTotal;

    public WebserviceStatistic incrementNotifiedTotal() {
        this.notifiedTotal++;
        return this;
    }
}
