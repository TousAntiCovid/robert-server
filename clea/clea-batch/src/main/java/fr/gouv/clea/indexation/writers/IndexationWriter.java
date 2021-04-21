package fr.gouv.clea.indexation.writers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import fr.gouv.clea.config.BatchProperties;
import fr.gouv.clea.indexation.model.output.ClusterFile;
import fr.gouv.clea.indexation.model.output.ClusterFileIndex;
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
import java.util.HashMap;
import java.util.List;

@StepScope
@Slf4j
public class IndexationWriter implements ItemWriter<HashMap<String, ClusterFile>> {

    private String outputPath;

    private Long jobId;

    @BeforeStep
    public void retrieveInterStepData(final StepExecution stepExecution) {
        this.jobId = stepExecution.getJobExecutionId();
    }

    public IndexationWriter(final BatchProperties config) {
        this.outputPath = config.clusterFilesOutputPath;
    }

    @Override
    public void write(final List<? extends HashMap<String, ClusterFile>> clusterIndexMap) throws Exception {

        log.info("Creating directories : " + outputPath + File.separator + this.jobId + File.separator);
        Files.createDirectories(Paths.get(outputPath + File.separator + this.jobId + File.separator));

        //generate index json file
        final HashMap<String, ClusterFile> hashMap = clusterIndexMap.get(0);
        generateClusterIndex(hashMap);

        //generate cluster files
        hashMap.forEach(this::generateClusterFile);
    }

    private void generateClusterFile(final String prefix, final ClusterFile clusterFile) {

        final String outputClusterFilePath = outputPath + File.separator + this.jobId + File.separator + prefix + ".json";
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

    private void generateClusterIndex(final HashMap<String, ClusterFile> clusterIndexMap) throws IOException {

        ClusterFileIndex clusterFileIndex = ClusterFileIndex.builder()
                .iteration(jobId.intValue())
                .prefixes(clusterIndexMap.keySet())
                .build();

        log.info("Generating cluster index : " + outputPath + File.separator + "clusterIndex.json");

        Path jsonPath = Paths.get(outputPath + File.separator + "clusterIndex.json");
        File jsonIndex = jsonPath.toFile();
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(jsonIndex, clusterFileIndex);
    }
}
