package fr.gouv.clea.indexation.index;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static fr.gouv.clea.config.BatchConstants.CLUSTER_INDEX_FILENAME;

@Slf4j
public class GenerateClusterIndexTasklet implements Tasklet {

    private final PrefixesStorageService prefixesStorageService;

    private final String outputPath;

    private final ObjectMapper objectMapper;

    public GenerateClusterIndexTasklet(final BatchProperties batchProperties, PrefixesStorageService prefixesStorageService, ObjectMapper objectMapper) {
        this.outputPath = batchProperties.getFilesOutputPath();
        this.prefixesStorageService = prefixesStorageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws IOException {
        final Long jobId = chunkContext.getStepContext().getJobInstanceId();

        log.info("Creating directories if not exists: " + outputPath + File.separator + jobId + File.separator);
        Files.createDirectories(Paths.get(outputPath + File.separator + jobId + File.separator));

        generateClusterIndex(jobId, prefixesStorageService.getPrefixWithAssociatedLtidsMap().keySet());
        return RepeatStatus.FINISHED;
    }

    void generateClusterIndex(final Long jobId, final Set<String> prefixes) throws IOException {

        ClusterFileIndex clusterFileIndex = ClusterFileIndex.builder()
                .iteration(jobId.intValue())
                .prefixes(prefixes)
                .build();

        log.info("Generating cluster index : " + outputPath + File.separator + CLUSTER_INDEX_FILENAME);

        Path jsonPath = Path.of(outputPath, CLUSTER_INDEX_FILENAME);
        File jsonIndex = jsonPath.toFile();
        objectMapper.writeValue(jsonIndex, clusterFileIndex);
    }
}
