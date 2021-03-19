package fr.gouv.tacw.services.impl;

import fr.gouv.tacw.services.IProcessService;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessServiceTest {

    private final IProcessService processService = new ProcessService();

    @BeforeEach
    void init() {
        assertThat(processService).isNotNull();
    }

}