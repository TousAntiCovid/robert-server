package fr.gouv.clea.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.clea.indexation.index.GenerateClusterIndexTasklet;
import fr.gouv.clea.service.PrefixesStorageService;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClusterIndexGenerationStepBatchConfig {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private PrefixesStorageService prefixesStorageService;

    @Autowired
    private BatchProperties batchProperties;

    @Bean
    public Step clusterIndexGeneration(final ObjectMapper objectMapper) {
        return stepBuilderFactory.get("clusterIndexGeneration")
                .tasklet(generateClusterIndex(objectMapper))
                .build();
    }

    @Bean
    public Tasklet generateClusterIndex(final ObjectMapper objectMapper) {
        return new GenerateClusterIndexTasklet(batchProperties, prefixesStorageService, objectMapper);
    }
}
