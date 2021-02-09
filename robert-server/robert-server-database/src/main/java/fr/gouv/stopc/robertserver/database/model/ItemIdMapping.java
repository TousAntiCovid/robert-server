package fr.gouv.stopc.robertserver.database.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Document(value = "itemIdMapping")
public class ItemIdMapping<T> {
    @Id
    private Long id;

    private T itemId;
}
