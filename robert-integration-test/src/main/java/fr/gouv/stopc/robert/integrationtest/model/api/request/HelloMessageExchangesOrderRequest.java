package fr.gouv.stopc.robert.integrationtest.model.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class HelloMessageExchangesOrderRequest {
    private String captchaId;

    private Integer frequencyInSeconds;

    private List<String> captchaIdOfOtherApps = new ArrayList<>();

    public HelloMessageExchangesOrderRequest captchaId(String captchaId) {
        this.captchaId = captchaId;
        return this;
    }
}
