package fr.gouv.clea.indexation.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import fr.gouv.clea.config.BatchProperties;
import fr.gouv.clea.indexation.model.output.ClusterFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static fr.gouv.clea.config.BatchConstants.JSON_FILE_EXTENSION;

@StepScope
@Slf4j
public class IndexationWriter implements ItemWriter<ClusterFile> {

    private final String rootDirectoryOutputPath;

    private final ObjectMapper objectMapper;

    private Long jobId;

    private Path jobDirectoryOutputPath;

    @BeforeStep
    public void retrieveInterStepData(final StepExecution stepExecution) {
        this.jobId = stepExecution.getJobExecutionId();
        this.jobDirectoryOutputPath = Path.of(this.rootDirectoryOutputPath, this.jobId.toString());
    }

    public IndexationWriter(final BatchProperties config, ObjectMapper objectMapper) {
        this.rootDirectoryOutputPath = config.getFilesOutputPath();
        this.objectMapper = objectMapper;
    }

    @Override
    public void write(List<? extends ClusterFile> clusterFile) throws Exception {

        log.info("Creating directories if not exists: {}", jobDirectoryOutputPath);
        Files.createDirectories(jobDirectoryOutputPath);


        clusterFile.forEach(clusterFile1 -> generateClusterFile(clusterFile1, jobDirectoryOutputPath));
    }

    private void generateClusterFile(final ClusterFile clusterFile, final Path directoryOutputPath) {

        final Path jsonClusterPath = Path.of(directoryOutputPath.toString(), clusterFile.getName()+JSON_FILE_EXTENSION);
        log.debug("Generating cluster file : {}", jsonClusterPath);
        File jsonClusterFile = jsonClusterPath.toFile();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            objectMapper.writeValue(jsonClusterFile, clusterFile.getItems());
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

}
