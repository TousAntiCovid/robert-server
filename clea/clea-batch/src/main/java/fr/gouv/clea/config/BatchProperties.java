package fr.gouv.clea.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;

@Getter
@Setter
@ToString
@NoArgsConstructor
@ConfigurationProperties(prefix = "clea.batch.cluster")
@Slf4j
public class BatchProperties {

    /**
     * Duration unit of a timeSlot
     */
    private int durationUnitInSeconds;

    private String filesOutputPath;

    private int staticPrefixLength;

    private int gridSize;

    private int identificationStepChunkSize;

    private int indexationStepChunkSize;

    private int prefixesComputingStepChunkSize;

    @PostConstruct
    private void logConfiguration() {
        log.info(this.toString());
    }
}
