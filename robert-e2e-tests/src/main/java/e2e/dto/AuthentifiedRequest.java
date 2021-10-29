package e2e.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
@Builder
public class AuthentifiedRequest {

    public static final String JSON_PROPERTY_EBID = "ebid";

    private byte[] ebid;

    public static final String JSON_PROPERTY_EPOCH_ID = "epochId";

    private Integer epochId;

    public static final String JSON_PROPERTY_TIME = "time";

    private byte[] time;

    public static final String JSON_PROPERTY_MAC = "mac";

    private byte[] mac;
}
