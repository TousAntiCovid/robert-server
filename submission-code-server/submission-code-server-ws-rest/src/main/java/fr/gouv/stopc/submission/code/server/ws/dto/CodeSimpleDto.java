package fr.gouv.stopc.submission.code.server.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
public class CodeSimpleDto {
    /**
     *  code generated formatted as long code or short code
     */
    private String code;

    /**
     *  Format ISO date is : YYYY-MM-DDTHH:mm:ss.sssZ
     */
    private String validFrom;

    /**
     *  Format ISO date is : YYYY-MM-DDTHH:mm:ss.sssZ
     */
    private String validUntil;

    /**
     * Format ISO date is : YYYY-MM-DDTHH:mm:ss.sssZ
     */
    private String dateGenerate;

}
