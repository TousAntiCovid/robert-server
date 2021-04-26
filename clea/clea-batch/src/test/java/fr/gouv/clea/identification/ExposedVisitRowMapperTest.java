package fr.gouv.clea.identification;

import fr.gouv.clea.entity.ExposedVisit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ExposedVisitRowMapperTest {

    @Mock
    private ResultSet resultSet;

    private ExposedVisitRowMapper mapper = new ExposedVisitRowMapper();

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void mapRow_returns_structured_object_with_values_from_resultSet() throws SQLException {
        final String rowId = "rowId";
        final int venueType = 2;
        final int venueCat1 = 1;
        final int venueCat2 = 3;
        final long periodStart = 50;
        final int timeSlot = 100000;
        final long backwardVisits = 5;
        final long forwardVisits = 6;
        String ltidString = "d3bfae7a-3ba9-4d28-8f30-6cdc3a46e426";

        when(resultSet.getString(eq("id"))).thenReturn(rowId);
        when(resultSet.getString(eq("ltid"))).thenReturn(ltidString);
        when(resultSet.getInt(eq("venue_type"))).thenReturn(venueType);
        when(resultSet.getInt(eq("venue_category1"))).thenReturn(venueCat1);
        when(resultSet.getInt(eq("venue_category2"))).thenReturn(venueCat2);
        when(resultSet.getLong(eq("period_start"))).thenReturn(periodStart);
        when(resultSet.getInt(eq("timeslot"))).thenReturn(timeSlot);
        when(resultSet.getLong(eq("backward_visits"))).thenReturn(backwardVisits);
        when(resultSet.getLong(eq("forward_visits"))).thenReturn(forwardVisits);

        final ExposedVisit visit = mapper.mapRow(resultSet, 0);
        assertThat(visit).as("visit").isNotNull();
        assertThat(visit.getId()).as("id").isEqualTo(rowId);
        assertThat(visit.getVenueCategory1()).as("venue_category1").isEqualTo(venueCat1);
        assertThat(visit.getVenueCategory2()).as("venue_category2").isEqualTo(venueCat2);
        assertThat(visit.getPeriodStart()).as("period_start").isEqualTo(periodStart);
        assertThat(visit.getTimeSlot()).as("timeslot").isEqualTo(timeSlot);
        assertThat(visit.getBackwardVisits()).as("backward_visits").isEqualTo(backwardVisits);
        assertThat(visit.getForwardVisits()).as("forward_visits").isEqualTo(forwardVisits);
        assertThat(visit.getLocationTemporaryPublicId()).as("ltid").isEqualTo(UUID.fromString(ltidString));

    }
}
