package fr.gouv.stopc.robertserver.database.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@Document(value = "kpis")
public class Kpi {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private Long value;

}
