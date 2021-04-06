package fr.gouv.clea.indexation.processors;

import fr.gouv.clea.config.BatchProperties;
import fr.gouv.clea.dto.ClusterPeriod;
import fr.gouv.clea.dto.SinglePlaceCluster;
import fr.gouv.clea.dto.SinglePlaceClusterPeriod;
import fr.gouv.clea.indexation.SinglePlaceClusterPeriodRowMapper;
import fr.gouv.clea.indexation.model.output.ClusterFile;
import fr.gouv.clea.indexation.model.output.ClusterFileItem;
import fr.gouv.clea.indexation.model.output.Prefix;
import fr.gouv.clea.mapper.SinglePlaceClusterPeriodMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static fr.gouv.clea.config.BatchConstants.LTID_COL;
import static fr.gouv.clea.config.BatchConstants.SINGLE_PLACE_CLUSTER_PERIOD_TABLE;

@Slf4j
public class SinglePlaceClusterBuilder implements ItemProcessor<Map.Entry<String, List<String>>, ClusterFile> {

    private final JdbcTemplate jdbcTemplate;
    private final SinglePlaceClusterPeriodMapper mapper;
    private final BatchProperties properties;

    AtomicLong counter = new AtomicLong();

    public SinglePlaceClusterBuilder(
            final DataSource dataSource,
            final SinglePlaceClusterPeriodMapper mapper,
            final BatchProperties properties) {

        jdbcTemplate = new JdbcTemplate(dataSource);
        this.mapper = mapper;
        this.properties = properties;
    }

    @Override
    public ClusterFile process(final Map.Entry<String, List<String>> ltids) {

        ClusterFile clusterFile = new ClusterFile();
        clusterFile.setName(Prefix.of(ltids.getValue().get(0), properties.prefixLength));

        ltids.getValue().forEach(ltid -> {
            final List<SinglePlaceClusterPeriod> clusterPeriodList = jdbcTemplate.query("select * from " + SINGLE_PLACE_CLUSTER_PERIOD_TABLE
                            + " WHERE ltid= ? ORDER BY " + LTID_COL,
                    new SinglePlaceClusterPeriodRowMapper(), UUID.fromString(ltid));
            SinglePlaceClusterPeriod singlePlaceClusterPeriod = clusterPeriodList.stream().findFirst().orElse(null);
            if (null != singlePlaceClusterPeriod) {
                long ln = counter.incrementAndGet();
                if (0 == ln % 1000) {
                    log.info("Loaded {} singlePlaceClusterPeriod, current LTId={} ", ln, ltid);
                }
                List<ClusterPeriod> clusterPeriods = clusterPeriodList.stream().map(mapper::map).collect(Collectors.toList());
                clusterFile.addItem(ClusterFileItem.ofCluster(SinglePlaceCluster.builder()
                        .locationTemporaryPublicId(singlePlaceClusterPeriod.getLocationTemporaryPublicId())
                        .venueCategory1(singlePlaceClusterPeriod.getVenueCategory1())
                        .venueCategory2(singlePlaceClusterPeriod.getVenueCategory2())
                        .venueType(singlePlaceClusterPeriod.getVenueType())
                        .periods(clusterPeriods)
                        .build()));
            }

        });
        log.info("Created cluterFile object containing {} items with prefix : {} ",
                ltids.getValue().size(),
                clusterFile.getName());

        return clusterFile;
    }
}
