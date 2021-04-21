package fr.gouv.clea.indexation.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.clea.config.BatchProperties;
import fr.gouv.clea.indexation.model.output.ClusterFileIndex;
import fr.gouv.clea.service.PrefixesStorageService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static fr.gouv.clea.config.BatchConstants.CLUSTER_INDEX_FILENAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GenerateClusterIndexTaskletTest {

    @Captor
    private ArgumentCaptor<ClusterFileIndex> clusterFileIndexCaptor;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private BatchProperties batchProperties;

    @Mock
    private PrefixesStorageService prefixesStorageService;


    @Test
    void generateClusterIndex_uses_objectMapper_to_create_clusterFileIndex_from_provided_prefixes() throws IOException {

        String outputPath = "outputPath";
        when(batchProperties.getFilesOutputPath()).thenReturn(outputPath);

        final GenerateClusterIndexTasklet tasklet = new GenerateClusterIndexTasklet(batchProperties, prefixesStorageService, objectMapper);
        final Long jobId = 1L;
        Set<String> prefixes = Set.of("prefix1", "prefix2", "prefix3");
        File jsonIndex = Path.of(outputPath, CLUSTER_INDEX_FILENAME).toFile();

        tasklet.generateClusterIndex(jobId, prefixes);

        verify(objectMapper, times(1)).writeValue(eq(jsonIndex), clusterFileIndexCaptor.capture());
        assertThat(clusterFileIndexCaptor.getValue().getPrefixes()).containsExactlyElementsOf(prefixes);
        assertThat(clusterFileIndexCaptor.getValue().getIteration()).isEqualTo(jobId.intValue());
    }
}
