package fr.gouv.stopc.submission.code.server.database.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Valid
public class SubmissionCodeDto {

    /*
        Empty for short codes, required for long codes
     */
    private long lot;

    @NotNull
    @NotBlank
    private String code;

    @NotNull
    private String type;

    @NotNull
    private OffsetDateTime dateEndValidity;

    @NotNull
    private OffsetDateTime dateAvailable;

    private OffsetDateTime dateUse;

    @NotNull
    private OffsetDateTime dateGeneration;

    @NotNull
    private Boolean used;
}
