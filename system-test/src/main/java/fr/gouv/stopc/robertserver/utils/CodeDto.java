package fr.gouv.stopc.robertserver.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CodeDto {

    /**
     * code generated formatted as long code or short code
     */
    private String code;

    /**
     * Format ISO date is : YYYY-MM-DDTHH:mm:ss.sssZ
     */
    private String validFrom;

    /**
     * Format ISO date is : YYYY-MM-DDTHH:mm:ss.sssZ
     */
    private String validUntil;

    /**
     * Format ISO date is : YYYY-MM-DDTHH:mm:ss.sssZ
     */
    private String dateGenerate;

}
