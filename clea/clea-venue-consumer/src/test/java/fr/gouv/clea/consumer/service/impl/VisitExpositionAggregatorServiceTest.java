package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.repository.IExposedVisitRepository;
import fr.gouv.clea.consumer.service.IVisitExpositionAggregatorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext
class VisitExpositionAggregatorServiceTest {

    @Autowired
    private IExposedVisitRepository repository;

    @Autowired
    private IVisitExpositionAggregatorService service;

    @BeforeEach
    void init() {

    }

    @AfterEach
    void clean() {

    }
}