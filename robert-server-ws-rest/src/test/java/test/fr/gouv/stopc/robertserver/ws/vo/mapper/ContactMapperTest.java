package test.fr.gouv.stopc.robertserver.ws.vo.mapper;

import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.Contact;
import fr.gouv.stopc.robertserver.ws.vo.ContactVo;
import fr.gouv.stopc.robertserver.ws.vo.HelloMessageDetailVo;
import fr.gouv.stopc.robertserver.ws.vo.mapper.ContactMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
public class ContactMapperTest {

    @InjectMocks
    private ContactMapper contactMapper;

    private final String sampleMac = "mac";

    private final String sampleEcc = "ecc";

    private final String sampleEbid = "ebid";

    private final int sampleRssiCalibrated = -127;

    @Test
    public void testMapEmptyListReturnsEmpty() {
        assertTrue(CollectionUtils.isEmpty(this.contactMapper.convertToEntity(Collections.emptyList())));
    }

    @Test
    public void testMapSizeSuccess() {
        ArrayList<ContactVo> contacts = new ArrayList<>();

        contacts.add(generateContact());
        contacts.add(generateContact());
        contacts.add(generateContact());

        int size = contacts.size();
        assertEquals(size, this.contactMapper.convertToEntity(contacts).size());
    }

    @Test
    public void testMapContentSuccess() {
        Optional<Contact> contact = this.contactMapper.convertToEntity(generateContact());

        assertTrue(contact.isPresent());
        assertTrue(Arrays.equals(sampleEbid.getBytes(), contact.get().getEbid()));
        assertTrue(Arrays.equals(sampleEcc.getBytes(), contact.get().getEcc()));
        assertFalse(CollectionUtils.isEmpty(contact.get().getMessageDetails()));
        assertTrue(Arrays.equals(sampleMac.getBytes(), contact.get().getMessageDetails().get(0).getMac()));
        assertEquals(sampleRssiCalibrated, contact.get().getMessageDetails().get(0).getRssiCalibrated());
    }

    private HelloMessageDetailVo generateHelloMessage() {
        return HelloMessageDetailVo.builder()
                .timeCollectedOnDevice(TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis()))
                .timeFromHelloMessage(1)
                .mac(Base64.getEncoder().encodeToString(sampleMac.getBytes()))
                .rssiCalibrated(sampleRssiCalibrated)
                .build();
    }

    private List<HelloMessageDetailVo> generateHelloMessages() {
        ArrayList<HelloMessageDetailVo> list = new ArrayList<>();
        list.add(generateHelloMessage());
        list.add(generateHelloMessage());
        list.add(generateHelloMessage());
        return list;
    }

    private ContactVo generateContact() {
        return ContactVo.builder()
                .ebid(Base64.getEncoder().encodeToString(sampleEbid.getBytes()))
                .ecc(Base64.getEncoder().encodeToString(sampleEcc.getBytes()))
                .ids(generateHelloMessages())
                .build();
    }
}
