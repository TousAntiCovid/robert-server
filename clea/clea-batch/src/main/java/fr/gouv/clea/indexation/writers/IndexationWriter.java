package fr.gouv.clea.indexation.writers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import fr.gouv.clea.config.BatchProperties;
import fr.gouv.clea.indexation.model.output.ClusterFile;
import fr.gouv.clea.indexation.model.output.ClusterFileIndex;
import fr.gouv.clea.indexation.model.output.ClusterFileItem;
import fr.gouv.clea.indexation.model.output.Prefix;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@StepScope
@Slf4j
public class IndexationWriter implements ItemWriter<ClusterFileItem> {

    private String outputPath;

    private Long jobId;

    private final int prefixLength;

    @BeforeStep
    public void retrieveInterStepData(final StepExecution stepExecution) {
        this.jobId = stepExecution.getJobExecutionId();
    }

    public IndexationWriter(final BatchProperties config) {
        this.outputPath = config.clusterFilesOutputPath;
        this.prefixLength = config.prefixLength;
    }

    @Override
    public void write(List<? extends ClusterFileItem> items) throws Exception {
        log.info("Creating directories : " + outputPath + File.separator + this.jobId + File.separator);
        Files.createDirectories(Paths.get(outputPath + File.separator + this.jobId + File.separator));

        //generate index json file
        final HashMap<String, ClusterFile> clusterIndexMap = new HashMap<>();

        items.forEach(clusterFileItem -> {
            final String uuid = clusterFileItem.getTemporaryLocationId();
            ClusterFile clusterFile = clusterIndexMap.computeIfAbsent(Prefix.of(uuid, prefixLength), key -> new ClusterFile());
            clusterFile.addItem(clusterFileItem);
        });
        //generate cluster files
        clusterIndexMap.forEach(this::generateClusterFile);

        generateClusterIndex(clusterIndexMap);
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
