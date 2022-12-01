package fr.gouv.stopc.e2e.mobileapplication.repository.model;

import lombok.Value;

@Value
public class Captcha {

    String id;

    String answer;
}
