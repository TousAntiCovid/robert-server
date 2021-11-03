package e2e.robert.ws.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.source.ConfigurationProperty;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RegisterSuccessResponse {

    public static final String JSON_PROPERTY_TUPLES = "tuples";

    private byte[] tuples;

    public static final String JSON_PROPERTY_TIME_START = "timeStart";

    private Long timeStart;

    public static final String JSON_PROPERTY_CONFIG = "config";

    private List<ConfigurationProperty> config = new ArrayList<>();

    public static final String JSON_PROPERTY_MESSAGE = "message";

    private String message;
}
