package e2e.robert.ws.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PushInfoVo {

    private String token;

    private String locale;

    private String timezone;

}
