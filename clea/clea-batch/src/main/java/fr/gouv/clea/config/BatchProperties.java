package fr.gouv.clea.config;

import lombok.Getter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@ToString
@Component
public class BatchProperties {

    /**
     * Duration unit of a timeSlot
     */
    @Value("${clea.batch.duration-unit-in-seconds}")
    public int durationUnitInSeconds;

    @Value("${clea.batch.cluster.files-output-path}")
    public String clusterFilesOutputPath;

    @Value("${clea.batch.cluster.static-prefix-length}")
    public int prefixLength;

    @Value("${clea.batch.cluster.grid-size}")
    private int gridSize;

    @Value("${clea.batch.cluster.chunk-size}")
    private int chunkSize;
}
