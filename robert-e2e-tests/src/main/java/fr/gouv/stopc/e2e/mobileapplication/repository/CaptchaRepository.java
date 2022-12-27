package fr.gouv.stopc.e2e.mobileapplication.repository;

import fr.gouv.stopc.e2e.mobileapplication.repository.model.Captcha;

public interface CaptchaRepository {

    Captcha saveRandomCaptcha();
}
