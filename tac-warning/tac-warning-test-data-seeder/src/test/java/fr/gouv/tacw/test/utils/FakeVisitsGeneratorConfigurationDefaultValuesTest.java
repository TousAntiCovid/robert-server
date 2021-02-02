package fr.gouv.tacw.test.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import fr.gouv.tacw.ws.vo.VenueCategoryVo;
import fr.gouv.tacw.ws.vo.VenueTypeVo;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = FakeVisitsGeneratorConfiguration.class)
public class FakeVisitsGeneratorConfigurationDefaultValuesTest {
    @Autowired
    FakeVisitsGeneratorConfiguration configuration;

    @Test
    public void testCanReadDefaultNbVenues() {
        assertThat(configuration.getNbVenues()).isEqualTo(80);
    }

    @Test
    public void testCanReadDefaultNbVisitors() {
        assertThat(configuration.getNbVisitors()).isEqualTo(5);
    }

    @Test
    public void testCanReadDefaultNbVisitsPerVisitorPerDay() {
        assertThat(configuration.getNbVisitsPerVisitorPerDay()).isEqualTo(4);
    }

    @Test
    public void testCanReadDefaultRetentionDays() {
        assertThat(configuration.getRetentionDays()).isEqualTo(12);
    }

    @Test
    public void testCanReadDefaultNbHangouts() {
        assertThat(configuration.getNbHangouts()).isEqualTo(8);
    }

    @Test
    public void testCanReadDefaultVenueType() {
        assertThat(configuration.getVenueType()).isEqualTo(VenueTypeVo.N);
    }
    
    @Test
    public void testCanReadDefaultVenueCategory() {
        assertThat(configuration.getVenueCategory()).isEqualTo(VenueCategoryVo.CAT5);
    }
}
