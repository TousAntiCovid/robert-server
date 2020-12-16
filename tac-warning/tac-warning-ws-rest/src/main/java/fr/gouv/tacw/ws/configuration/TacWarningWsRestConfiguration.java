package fr.gouv.tacw.ws.configuration;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix="tacw.rest")
@Configuration
public class TacWarningWsRestConfiguration {
    /* Salt is used to randomize the UUID */
    @Min(value = 1)
    private int maxSalt;
    
    /* Time to substract to the visit time to get the start of the visit */
    @Min(value = 0)
    private int startDelta;
    
    /* Time to add to the visit time to get the end of the visit */
    @Min(value = 0)
    private int endDelta;
    
    /* If true, authorization for wreport is disabled */
    private boolean jwtReportAuthorizationDisabled;
    
    /* The JWT public key to used to verify token signature */
    private String robertJwtPublicKey;
    
    /* Maximum number of visits accepted by a single wstatus or wreport request */
    @Min(value = 1)
    private int maxVisits;
    
    /* If score is greater than threshold, reporter becomes at risk */
    @Min(value = 1)
    private int scoreThreshold;
    
    /* The increment for a new Covid+ for a given venue category */
    private Map<String,Integer> exposureCountIncrements = new HashMap<String, Integer>();
}
