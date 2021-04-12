package fr.gouv.clea.identification.processor;

import fr.gouv.clea.dto.SinglePlaceExposedVisits;
import fr.gouv.clea.entity.ExposedVisit;
import fr.gouv.clea.identification.ExposedVisitRowMapper;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static fr.gouv.clea.config.BatchConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SinglePlaceExposedVisitsBuilderTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ExposedVisitRowMapper rowMapper;

    @InjectMocks
    private SinglePlaceExposedVisitsBuilder builder;

    @Test
    void builder_processes_ltid_and_returns_object_with_agregated_visits_list() {
        final String ltid = "e9d58612-993f-11eb-a8b3-0242ac130003";
        final UUID ltidUUID = UUID.fromString(ltid);

        final int venueType = 2;
        final int vCat1 = 1;
        final int vCat2 = 2;
        // only v1 is detailed because only this one's information are queried to build SinglePlaceExposedVisits
        final ExposedVisit v1 = ExposedVisit.builder()
                .locationTemporaryPublicId(ltidUUID)
                .venueType(venueType)
                .venueCategory1(vCat1)
                .venueCategory2(vCat2)
                .build();
        final ExposedVisit v2 = new ExposedVisit();

        final String sql = "select * from " + EXPOSED_VISITS_TABLE
                + " WHERE ltid= ? ORDER BY " + PERIOD_COLUMN + ", " + TIMESLOT_COLUMN;
        when(jdbcTemplate.query(sql, rowMapper, ltidUUID)).thenReturn(List.of(v1, v2));

        final SinglePlaceExposedVisits processResult = builder.process(ltid);

        assertThat(processResult).isNotNull();
        assertThat(processResult.getLocationTemporaryPublicId()).isEqualTo(ltidUUID);
        assertThat(processResult.getVenueCategory1()).isEqualTo(vCat1);
        assertThat(processResult.getVenueCategory2()).isEqualTo(vCat2);
        assertThat(processResult.getVisits()).containsExactly(v1, v2);
        assertThat(processResult.getVenueType()).isEqualTo(venueType);
    }

    @Test
    void builder_returns_null_when_ltid_does_not_match_any_base_entry() {
        final String ltid = "e9d58612-993f-11eb-a8b3-0242ac130003";
        final UUID ltidUUID = UUID.fromString(ltid);

        final String sql = "select * from " + EXPOSED_VISITS_TABLE
                + " WHERE ltid= ? ORDER BY " + PERIOD_COLUMN + ", " + TIMESLOT_COLUMN;
        when(jdbcTemplate.query(sql, rowMapper, ltidUUID)).thenReturn(List.of());

        final SinglePlaceExposedVisits processResult = builder.process(ltid);

        assertThat(processResult).isNull();
    }
}
