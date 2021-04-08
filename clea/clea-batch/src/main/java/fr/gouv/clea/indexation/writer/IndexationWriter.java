package fr.gouv.clea.indexation.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import fr.gouv.clea.config.BatchProperties;
import fr.gouv.clea.indexation.model.output.ClusterFile;
import fr.gouv.clea.indexation.model.output.ClusterFileIndex;
import fr.gouv.clea.service.PrefixesStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

@StepScope
@Slf4j
public class IndexationWriter implements ItemWriter<ClusterFile> {

    private String outputPath;

    private Long jobId;

    @BeforeStep
    public void retrieveInterStepData(final StepExecution stepExecution) {
        this.jobId = stepExecution.getJobExecutionId();
    }

    public IndexationWriter(final BatchProperties config) {
        this.outputPath = config.getFilesOutputPath();
    }

    @Override
    public void write(List<? extends ClusterFile> clusterFile) throws Exception {

        log.info("Creating directories : " + outputPath + File.separator + this.jobId + File.separator);
        Files.createDirectories(Paths.get(outputPath + File.separator + this.jobId + File.separator));


        clusterFile.forEach(this::generateClusterFile);
    }

    private void generateClusterFile(final ClusterFile clusterFile) {

        final String outputClusterFilePath = outputPath + File.separator + this.jobId + File.separator + clusterFile.getName() + ".json";
        log.debug("Generating cluster file : {}", outputClusterFilePath);
        Path jsonClusterPath = Paths.get(outputClusterFilePath);
        File jsonClusterFile = jsonClusterPath.toFile();
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            mapper.writeValue(jsonClusterFile, clusterFile.getItems());
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

}
