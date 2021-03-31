package fr.gouv.tac.analytics.server.model.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Document(value = "tokenIdentifier")
public class TokenIdentifier {

    public static final String EXPIRATION_DATE_FIELD_NAME = "expirationDate";
    public static final String IDENTIFIER_FIELD_NAME = "identifier";

    @Id
    private String id;

    @NotNull
    @Field(name = IDENTIFIER_FIELD_NAME)
    private String identifier;

    @NotNull
    @Field(name = EXPIRATION_DATE_FIELD_NAME)
    private ZonedDateTime expirationDate;
}
