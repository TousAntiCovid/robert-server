package fr.gouv.stopc.robert.integrationtests.config;

import lombok.AllArgsConstructor;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URL;

@Value
@Valid
@AllArgsConstructor
public class RobertWsRestProperties {

    @NotNull
    URL baseUrl;
}
