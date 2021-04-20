package fr.gouv.clea.clea.scoring.configuration.risk;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesBinding
public class RiskConfigurationConverter implements Converter<String, RiskRule>{

    public static final String WILDCARD = "*";

    @Override
    public RiskRule convert(String source) {
        String[] data = source.split(",");
        return RiskRule.builder()
            .venueType(this.stringToInt(data[0]))
            .venueCategory1(this.stringToInt(data[1]))
            .venueCategory2(this.stringToInt(data[2]))
            .clusterThresholdBackward(Integer.parseInt(data[3]))
            .clusterThresholdForward(Integer.parseInt(data[4]))
            .riskLevelBackward(Float.parseFloat(data[5]))
            .riskLevelForward(Float.parseFloat(data[6]))
            .build(); 
    }
    
    public int stringToInt(String s) {
        return s.equals(WILDCARD) ? RiskRule.WILDCARD_VALUE : Integer.parseInt(s);
    }

}
