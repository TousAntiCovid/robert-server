package fr.gouv.clea.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ToString
@NoArgsConstructor
@ConfigurationProperties(prefix = "clea.batch.cluster")
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
}
