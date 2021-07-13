package fr.gouv.stopc.robert.integrationtest.model.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.stopc.robert.integrationtest.model.api.request.PushInfoVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
public class RegisterVo {

//    {
//        "captcha": "string",
//        "captchaId": "string",
//        "clientPublicECDHKey": "string",
//        "pushInfo": {
//            "token": "string",
//                    "locale": "string",
//                    "timezone": "string"
//            }
//    }

  @JsonProperty(required = true)
  @NotNull
  @NotEmpty
  @ToString.Exclude
  private String captchaId;

  @JsonProperty(required = true)
  @NotNull
  @NotEmpty
  @ToString.Exclude
  private String captcha;

  @JsonProperty(required = true)
  @NotNull
  @NotEmpty
  @ToString.Exclude
  private String clientPublicECDHKey;

  @ToString.Exclude
  private PushInfoVo pushInfo;

}
