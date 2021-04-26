package fr.gouv.clea.identification;

import fr.gouv.clea.entity.ExposedVisit;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ExposedVisitRowMapper implements RowMapper<ExposedVisit> {
    @Override
    public ExposedVisit mapRow(ResultSet rs, int i) throws SQLException {
        return ExposedVisit.builder()
                .id(rs.getString("id"))
                .locationTemporaryPublicId(UUID.fromString(rs.getString("ltid")))
                .venueType(rs.getInt("venue_type"))
                .venueCategory1(rs.getInt("venue_category1"))
                .venueCategory2(rs.getInt("venue_category2"))
                .periodStart(rs.getLong("period_start"))
                .timeSlot(rs.getInt("timeslot"))
                .backwardVisits(rs.getLong("backward_visits"))
                .forwardVisits(rs.getLong("forward_visits"))
                .build();
    }
}
