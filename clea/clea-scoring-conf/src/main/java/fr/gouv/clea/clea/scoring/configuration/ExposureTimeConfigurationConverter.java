package fr.gouv.clea.clea.scoring.configuration;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesBinding
public class ExposureTimeConfigurationConverter implements Converter<String, ExposureTimeConfiguration>{

    public static final String WILDCARD = "*";

    @Override
    public ExposureTimeConfiguration convert(String source) {
        String[] data = source.split(",");
        return ExposureTimeConfiguration.builder()
            .venueType(this.stringToInt(data[0]))
            .venueCategory1(this.stringToInt(data[1]))
            .venueCategory2(this.stringToInt(data[2]))
            .exposureTime(Integer.parseInt(data[3]))
            .build(); 
    }
    
    public int stringToInt(String s) {
        return s.equals(WILDCARD) ? ExposureTimeConfiguration.wildcardValue : Integer.parseInt(s);
    }

}
