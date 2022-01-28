package fr.gouv.stopc.robertserver.ws.service.impl;

import fr.gouv.stopc.robertserver.database.model.Contact;
import fr.gouv.stopc.robertserver.database.service.ContactService;
import fr.gouv.stopc.robertserver.ws.exception.RobertServerException;
import fr.gouv.stopc.robertserver.ws.service.ContactDtoService;
import fr.gouv.stopc.robertserver.ws.utils.MessageConstants;
import fr.gouv.stopc.robertserver.ws.vo.ContactVo;
import fr.gouv.stopc.robertserver.ws.vo.mapper.ContactMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactDtoServiceImpl implements ContactDtoService {

    private final ContactService contactService;

    private final ContactMapper contactMapper;

    @Override
    public void saveContacts(List<ContactVo> contactVoList) throws RobertServerException {

        try {
            List<Contact> contacts = contactMapper.convertToEntity(contactVoList);

            contactService.saveContacts(contacts);
        } catch (Exception e) {

            throw new RobertServerException(MessageConstants.ERROR_OCCURED, e);
        }
    }

}
