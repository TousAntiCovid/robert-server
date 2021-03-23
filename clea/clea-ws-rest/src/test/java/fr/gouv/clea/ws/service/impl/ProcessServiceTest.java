package fr.gouv.clea.ws.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;

import fr.gouv.clea.ws.service.IProcessService;

class ProcessServiceTest {

    private final IProcessService processService = new ProcessService();

    @BeforeEach
    void init() {
        assertThat(processService).isNotNull();
    }

}