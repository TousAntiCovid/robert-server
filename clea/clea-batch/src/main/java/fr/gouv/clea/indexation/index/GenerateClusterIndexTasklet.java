package fr.gouv.clea.indexation.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import fr.gouv.clea.config.BatchProperties;
import fr.gouv.clea.indexation.model.output.ClusterFileIndex;
import fr.gouv.clea.service.PrefixesStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

@Slf4j
public class GenerateClusterIndexTasklet implements Tasklet {

    private final PrefixesStorageService prefixesStorageService;

    private final String outputPath;

    public GenerateClusterIndexTasklet(final BatchProperties batchProperties, PrefixesStorageService prefixesStorageService) {
        this.outputPath = batchProperties.getFilesOutputPath();
        this.prefixesStorageService = prefixesStorageService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws IOException {
        final Long jobId = chunkContext.getStepContext().getJobInstanceId();

        log.info("Creating directories if not exists: " + outputPath + File.separator + jobId + File.separator);
        Files.createDirectories(Paths.get(outputPath + File.separator + jobId + File.separator));

        generateClusterIndex(jobId, prefixesStorageService.getPrefixWithAssociatedLtidsMap().keySet());
        return RepeatStatus.FINISHED;
    }

    private void generateClusterIndex(final Long jobId, final Set<String> prefixes) throws IOException {

        ClusterFileIndex clusterFileIndex = ClusterFileIndex.builder()
                .iteration(jobId.intValue())
                .prefixes(prefixes)
                .build();

        log.info("Generating cluster index : " + outputPath + File.separator + "clusterIndex.json");

        Path jsonPath = Paths.get(outputPath + File.separator + "clusterIndex.json");
        File jsonIndex = jsonPath.toFile();
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(jsonIndex, clusterFileIndex);
    }
}
