package fr.gouv.tacw.test.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import fr.gouv.tacw.ws.vo.VenueCategoryVo;
import fr.gouv.tacw.ws.vo.VenueTypeVo;

@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:application.properties")
@EnableConfigurationProperties(value = FakeVisitsGeneratorConfiguration.class)
public class FakeVisitsGeneratorConfigurationTest {
    @Autowired
    FakeVisitsGeneratorConfiguration configuration;

    @Test
    public void testCanReadNbVenues() {
        assertThat(configuration.getNbVenues()).isEqualTo(1400);
    }

    @Test
    public void testCanReadNbVisitors() {
        assertThat(configuration.getNbVisitors()).isEqualTo(45000);
    }

    @Test
    public void testCanReadNbVisitsPerVisitorPerDay() {
        assertThat(configuration.getNbVisitsPerVisitorPerDay()).isEqualTo(5);
    }

    @Test
    public void testCanReadRetentionDays() {
        assertThat(configuration.getRetentionDays()).isEqualTo(12);
    }

    @Test
    public void testCanReadNbHangouts() {
        assertThat(configuration.getNbHangouts()).isEqualTo(8);
    }

    @Test
    public void testCanReadVenueType() {
        assertThat(configuration.getVenueType()).isEqualTo(VenueTypeVo.N);
    }
    
    @Test
    public void testCanReadVenueCategory() {
        assertThat(configuration.getVenueCategory()).isEqualTo(VenueCategoryVo.CAT1);
    }
}
