package fr.gouv.clea.indexation;

import fr.gouv.clea.dto.SinglePlaceClusterPeriod;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class SinglePlaceClusterPeriodRowMapper implements RowMapper<SinglePlaceClusterPeriod> {

    @Override
    public SinglePlaceClusterPeriod mapRow(ResultSet rs, int rowNum) throws SQLException {
        return SinglePlaceClusterPeriod.builder()
                .locationTemporaryPublicId(UUID.fromString(rs.getString("ltid")))
                .venueType(rs.getInt("venue_type"))
                .venueCategory1(rs.getInt("venue_category1"))
                .venueCategory2(rs.getInt("venue_category2"))
                .periodStart(rs.getLong("period_start"))
                .firstTimeSlot(rs.getInt("first_timeslot"))
                .lastTimeSlot(rs.getInt("last_timeslot"))
                .clusterStart(rs.getLong("cluster_start"))
                .clusterDurationInSeconds(rs.getInt("cluster_duration_in_seconds"))
                .riskLevel(rs.getFloat("risk_level"))
                .build();
    }
}