package fr.gouv.stopc.robert.server.batch.processor;

import fr.gouv.stopc.robert.server.batch.RobertServerBatchApplication;
import fr.gouv.stopc.robert.server.batch.utils.MetricsService;
import fr.gouv.stopc.robertserver.database.model.Contact;
import fr.gouv.stopc.robertserver.database.model.Registration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { RobertServerBatchApplication.class })
@TestPropertySource(locations = "classpath:application.properties", properties = "robert.scoring.algo-version=0")
public class ItemIdProcessorTest {

    private ContactIdMappingProcessor contactIdMappingProcessor;

    private RegistrationIdMappingProcessor registrationIdMappingProcessor;

    @Autowired
    private MetricsService metricsService;

    @BeforeEach
    public void before() {
        this.contactIdMappingProcessor = new ContactIdMappingProcessor(metricsService);
        this.registrationIdMappingProcessor = new RegistrationIdMappingProcessor();
    }

    @Test
    public void testBuilContactEntryForIdMapping() {
        // Given
        final var contact1 = Contact.builder().id("a971").build();
        final var contact2 = Contact.builder().id("a972").build();

        // When
        final var process1 = contactIdMappingProcessor.process(contact1);
        final var process2 = contactIdMappingProcessor.process(contact2);

        final var sequentialId1 = process1.getId();
        final var sequentialId2 = process2.getId();

        // Then
        assertNotNull(process1.getId());
        assertNotNull(process2.getId());
        assertNotNull(process1.getItemId());
        assertNotNull(process2.getItemId());

        assertEquals(sequentialId1 + 1, sequentialId2);
    }

    @Test
    public void testBuilRegistrationEntryForIdMapping() {
        // Given
        final var sr = new SecureRandom();
        byte[] rndBytes1 = new byte[5];
        sr.nextBytes(rndBytes1);

        byte[] rndBytes2 = new byte[5];
        sr.nextBytes(rndBytes2);

        final var registration1 = Registration.builder().permanentIdentifier(rndBytes1).build();
        final var registration2 = Registration.builder().permanentIdentifier(rndBytes2).build();

        // When
        final var process1 = registrationIdMappingProcessor.process(registration1);
        final var process2 = registrationIdMappingProcessor.process(registration2);

        final var sequentialId1 = process1.getId();
        final var sequentialId2 = process2.getId();

        // Then
        assertNotNull(sequentialId1);
        assertNotNull(sequentialId2);
        assertNotNull(process1.getItemId());
        assertNotNull(process2.getItemId());

        assertEquals(sequentialId1 + 1, sequentialId2);
    }
}
