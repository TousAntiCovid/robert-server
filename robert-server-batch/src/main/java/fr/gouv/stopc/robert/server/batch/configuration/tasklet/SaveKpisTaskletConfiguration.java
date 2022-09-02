package fr.gouv.stopc.robert.server.batch.configuration.tasklet;

import fr.gouv.stopc.robert.server.batch.tasklet.SaveKpisTasklet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class SaveKpisTaskletConfiguration {

    private final SaveKpisTasklet saveKpisTasklet;

    @Bean
    public Step saveKpisStep(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("saveKpisTasklet")
                .tasklet(saveKpisTasklet)
                .build();
    }
}
