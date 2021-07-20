package fr.gouv.stopc.robert.integrationtest.model.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
public class ReportResponse {
    private Boolean success;
    private String message;
    private String reportValidationToken;
}
