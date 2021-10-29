package e2e.dto;

import lombok.*;

import javax.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PushInfoVo {

    @NotNull
    @ToString.Exclude
    private String token;

    @NotNull
    @ToString.Exclude
    private String locale;

    @NotNull
    @ToString.Exclude
    private String timezone;

}
