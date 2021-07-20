package fr.gouv.stopc.robert.integrationtest.model.api.request;

import fr.gouv.stopc.robert.integrationtest.model.Contact;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
public class ReportRequest {

    @NotNull
    private String token;

    @NotNull
    private List<Contact> contacts = null;

    @NotNull
    private byte[] contactsAsBinary;
}
