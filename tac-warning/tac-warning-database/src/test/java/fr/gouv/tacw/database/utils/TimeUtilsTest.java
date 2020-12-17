package fr.gouv.tacw.database.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class TimeUtilsTest {
    @Test
    public void testTimestampIsRoundedDownToTheNextLongWhenModuloLowerThanHalfTimeRoundingValue() {
        assertThat(TimeUtils.roundedTimestamp(1606132850)).isEqualTo(1606132800);
    }

    @Test
    public void testTimestampIsRoundedUpToTheNextLongWhenModuloGreaterThanHalfTimeRoundingValue() {
        assertThat(TimeUtils.roundedTimestamp(1606136000)).isEqualTo(1606136400);
    }

    @Test
    public void testTimestampIsNotRoundedWhenMultipleOfTimeRoundingValue() {
        assertThat(TimeUtils.roundedTimestamp(1606140000)).isEqualTo(1606140000);
    }

    @Test
    public void testTimestampIsRoundedUpToTheNextLongWhenModuloIsEqualToHalfTimeRoundingValue() {
        assertThat(TimeUtils.roundedTimestamp(1606140450)).isEqualTo(1606140900);
    }
}
