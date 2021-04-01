package fr.gouv.clea.mapper;

import fr.gouv.clea.dto.ClusterPeriod;
import fr.gouv.clea.dto.SinglePlaceCluster;
import fr.gouv.clea.dto.SinglePlaceClusterPeriod;
import org.mapstruct.Mapper;

@Mapper
public interface SinglePlaceClusterPeriodMapper {

    SinglePlaceClusterPeriod map(SinglePlaceCluster cluster, ClusterPeriod period);

    ClusterPeriod map(SinglePlaceClusterPeriod singlePlaceClusterPeriod);
}
