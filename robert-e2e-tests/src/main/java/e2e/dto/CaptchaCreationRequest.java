package e2e.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CaptchaCreationRequest {

    @NotNull
    private String type;

    @NotNull
    private String locale;
}
