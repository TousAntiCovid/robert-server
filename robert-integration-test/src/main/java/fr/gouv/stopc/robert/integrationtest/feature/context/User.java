package fr.gouv.stopc.robert.integrationtest.feature.context;



import fr.gouv.stopc.robert.integrationtest.model.AppMobile;
import fr.gouv.stopc.robert.integrationtest.model.Contact;
import fr.gouv.stopc.robert.integrationtest.model.api.response.ExposureStatusResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@RequiredArgsConstructor
public class User {

    private final String name;

    private String captchaId;

    private String captchaSolution;

    private String clientPublicECDHKey;

    private ExposureStatusResponse lastExposureStatusResponse;

    private List<Contact> contactList = new ArrayList();

}
