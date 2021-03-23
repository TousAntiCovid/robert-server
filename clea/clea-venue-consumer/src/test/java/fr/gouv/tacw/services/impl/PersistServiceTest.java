package fr.gouv.tacw.services.impl;

import fr.gouv.tacw.data.IDetectedVenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PersistServiceTest {

    @Autowired
    private IDetectedVenueRepository repository;

    @BeforeEach
    void init() {
        assertThat(repository).isNotNull();
    }

    @Test
    @DisplayName("")
    void checkAndPersist() {

    }

}