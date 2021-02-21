package fr.gouv.stopc.submission.code.server.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class VerifyDto {

    /**
     * True if the code is valid, false if invalid or expired, or if type does not match.
     *
     * A valid code is destroyed if validated successfully.
     */
    boolean valid;
}
