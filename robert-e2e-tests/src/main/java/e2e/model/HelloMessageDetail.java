package e2e.model;

import lombok.*;

import javax.validation.constraints.NotNull;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class HelloMessageDetail {

    /**
     * WARNING: Since Java does not support unsigned int, we are obligated to store
     * the value coming from the JSON representation as a Long even though it should
     * be used as an unsigned int.
     */
    @NotNull
    @ToString.Exclude
    private Long timeCollectedOnDevice;

    /**
     * WARNING: Since Java does not support unsigned short, we are obligated to
     * store the value coming from the JSON representation as an Integer even though
     * it should be used as an unsigned short.
     */
    @NotNull
    @ToString.Exclude
    private Integer timeFromHelloMessage;

    @NotNull
    @ToString.Exclude
    private byte[] mac;

    @NotNull
    private Integer rssiCalibrated;

}
