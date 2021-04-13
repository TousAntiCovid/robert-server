package fr.gouv.clea.indexation.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import fr.gouv.clea.config.BatchProperties;
import fr.gouv.clea.indexation.model.output.ClusterFile;
import fr.gouv.clea.indexation.model.output.ClusterFileIndex;
import fr.gouv.clea.indexation.model.output.ClusterFileItem;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static fr.gouv.clea.config.BatchConstants.JSON_FILE_EXTENSION;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class IndexationWriterTest {

    @Mock
    private BatchProperties batchProperties;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private IndexationWriter writer;

    @Test
    void generateClusterFile_writes_files_through_objectMapper_with_expected_directory_name() throws IOException {

        final ClusterFile clusterFile = new ClusterFile();
        clusterFile.setName("name");
        final String ltid = "ltid";
        final ClusterFileItem clusterFileItem = ClusterFileItem.builder()
                .temporaryLocationId(ltid)
                .build();
        clusterFile.setItems(List.of(clusterFileItem));
        String directoryOutputPath = "outputPath";

        writer.generateClusterFile(clusterFile, Path.of(directoryOutputPath));

        final File expectedFilePath = Path.of(directoryOutputPath, clusterFile.getName() + JSON_FILE_EXTENSION).toFile();
        Mockito.verify(objectMapper, Mockito.times(1)).enable(SerializationFeature.INDENT_OUTPUT);
        Mockito.verify(objectMapper, Mockito.times(1)).writeValue(expectedFilePath, clusterFile.getItems());
    }
}
