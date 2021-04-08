package fr.gouv.clea.indexation.processor;

import fr.gouv.clea.config.BatchProperties;
import fr.gouv.clea.dto.ClusterPeriod;
import fr.gouv.clea.dto.SinglePlaceCluster;
import fr.gouv.clea.dto.SinglePlaceClusterPeriod;
import fr.gouv.clea.indexation.SinglePlaceClusterPeriodRowMapper;
import fr.gouv.clea.indexation.model.output.ClusterFile;
import fr.gouv.clea.indexation.model.output.ClusterFileItem;
import fr.gouv.clea.indexation.model.output.Prefix;
import fr.gouv.clea.mapper.ClusterPeriodModelsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static fr.gouv.clea.config.BatchConstants.SINGLE_PLACE_CLUSTER_PERIOD_TABLE;

@Slf4j
public class SinglePlaceClusterBuilder implements ItemProcessor<Map.Entry<String, List<String>>, ClusterFile> {

    private final JdbcTemplate jdbcTemplate;
    private final ClusterPeriodModelsMapper mapper;
    private final BatchProperties properties;

    public SinglePlaceClusterBuilder(
            final DataSource dataSource,
            final ClusterPeriodModelsMapper mapper,
            final BatchProperties properties) {

        jdbcTemplate = new JdbcTemplate(dataSource);
        this.mapper = mapper;
        this.properties = properties;
    }

    @Override
    public ClusterFile process(final Map.Entry<String, List<String>> prefixLtidsEntry) {
        log.debug("Processing prefix {} files...", prefixLtidsEntry.getKey());
        
        ClusterFile clusterFile = new ClusterFile();
        clusterFile.setName(Prefix.of(prefixLtidsEntry.getValue().get(0), properties.getStaticPrefixLength()));
        
        prefixLtidsEntry.getValue().forEach(createClusterFile(clusterFile));
        return clusterFile;
    }

    private Consumer<String> createClusterFile(final ClusterFile clusterFile) {
        return ltid -> {
            final List<SinglePlaceClusterPeriod> singlePlacePeriodsList = queryForSinglePlaceClusterPeriods(ltid);
            // Verify if at least one period is present, and if so, proceed
            singlePlacePeriodsList.stream().findFirst().ifPresent(firstPeriod -> {
                List<ClusterPeriod> clusterPeriods = buildClusterPeriods(singlePlacePeriodsList);
                clusterFile.addItem(createClusterFileItem(firstPeriod, clusterPeriods));
            });
        };
    }

    private ClusterFileItem createClusterFileItem(SinglePlaceClusterPeriod firstPeriod, List<ClusterPeriod> clusterPeriods) {
        return ClusterFileItem.ofCluster(SinglePlaceCluster.builder()
                .locationTemporaryPublicId(firstPeriod.getLocationTemporaryPublicId())
                .venueCategory1(firstPeriod.getVenueCategory1())
                .venueCategory2(firstPeriod.getVenueCategory2())
                .venueType(firstPeriod.getVenueType())
                .periods(clusterPeriods)
                .build());
    }

    private List<SinglePlaceClusterPeriod> queryForSinglePlaceClusterPeriods(final String ltid) {
        return jdbcTemplate.query("select * from " + SINGLE_PLACE_CLUSTER_PERIOD_TABLE + " WHERE ltid= ?",
                new SinglePlaceClusterPeriodRowMapper(), UUID.fromString(ltid));
    }

    private List<ClusterPeriod> buildClusterPeriods(final List<SinglePlaceClusterPeriod> clusterPeriodList) {
        return clusterPeriodList.stream().map(mapper::map).collect(Collectors.toList());
    }
}
