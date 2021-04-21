package fr.gouv.clea.clea.scoring.configuration.exposure;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesBinding
public class ExposureTimeConfigurationConverter implements Converter<String, ExposureTimeRule>{

    public static final String WILDCARD = "*";

    @Override
    public ExposureTimeRule convert(String source) {
        String[] data = source.split(",");
        return ExposureTimeRule.builder()
            .venueType(this.stringToInt(data[0]))
            .venueCategory1(this.stringToInt(data[1]))
            .venueCategory2(this.stringToInt(data[2]))
            .exposureTimeBackward(Integer.parseInt(data[3]))
            .exposureTimeForward(Integer.parseInt(data[4]))
            .exposureTimeStaffBackward(Integer.parseInt(data[5]))
            .exposureTimeStaffForward(Integer.parseInt(data[6]))
            .build(); 
    }
    
    public int stringToInt(String s) {
        return s.equals(WILDCARD) ? ExposureTimeRule.WILDCARD_VALUE : Integer.parseInt(s);
    }

}
