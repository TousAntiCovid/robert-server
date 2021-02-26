package fr.gouv.stopc.submission.code.server.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
public class CodeDetailedDto extends CodeSimpleDto {

    /**
     * TODO rename ?
     *  UUIDv4 or 6-alphanum
     */
    private String typeAsString;

    /**
     *
     * 1  - > long code
     * 2  - > short code
     */
    private Integer typeAsInt;

}
