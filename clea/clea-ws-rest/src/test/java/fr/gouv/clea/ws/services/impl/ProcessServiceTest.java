package fr.gouv.clea.ws.services.impl;

import org.junit.jupiter.api.BeforeEach;

import fr.gouv.clea.ws.service.IProcessService;
import fr.gouv.clea.ws.service.impl.ProcessService;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessServiceTest {

    private final IProcessService processService = new ProcessService();

    @BeforeEach
    void init() {
        assertThat(processService).isNotNull();
    }

}