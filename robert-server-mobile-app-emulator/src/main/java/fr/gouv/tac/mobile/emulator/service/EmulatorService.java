package fr.gouv.tac.mobile.emulator.service;

import fr.gouv.stopc.robert.server.common.DigestSaltEnum;
import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.tac.mobile.emulator.api.model.HelloMessageExchangesOrderRequest;
import fr.gouv.tac.mobile.emulator.api.model.RegisterOrderRequest;
import fr.gouv.tac.mobile.emulator.api.model.ReportOrderRequest;
import fr.gouv.tac.mobile.emulator.config.EmulatorProperties;
import fr.gouv.tac.mobile.emulator.model.AppMobile;
import fr.gouv.tac.mobile.emulator.robert.api.DefaultApi;
import fr.gouv.tac.mobile.emulator.robert.api.model.AuthentifiedRequest;
import fr.gouv.tac.mobile.emulator.robert.api.model.ExposureStatusRequest;
import fr.gouv.tac.mobile.emulator.robert.api.model.ExposureStatusResponse;
import fr.gouv.tac.mobile.emulator.robert.api.model.RegisterRequest;
import fr.gouv.tac.mobile.emulator.robert.api.model.RegisterSuccessResponse;
import fr.gouv.tac.mobile.emulator.robert.api.model.ReportBatchRequest;
import fr.gouv.tac.mobile.emulator.robert.api.model.UnregisterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EmulatorService {

    private final EmulatorProperties emulatorProperties;

    private final Map<String, AppMobile> appMobileMap;

    private final DefaultApi robertApiClient;

    public EmulatorService(DefaultApi robertApiClient, EmulatorProperties emulatorProperties) {
        this.emulatorProperties = emulatorProperties;
        this.robertApiClient = robertApiClient;
        robertApiClient.getApiClient().setBasePath(emulatorProperties.getRobertWsUrl());
        this.appMobileMap = new HashMap<>();
    }

    public void register(RegisterOrderRequest registerOrderRequest)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {

        AppMobile appMobile = new AppMobile(
                registerOrderRequest.getCaptchaId(), registerOrderRequest.getCaptcha(),
                emulatorProperties.getRobertCryptoPublicKey()
        );

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setCaptcha(registerOrderRequest.getCaptcha());
        registerRequest.setCaptchaId(registerOrderRequest.getCaptchaId());
        registerRequest.setClientPublicECDHKey(appMobile.getPublicKey());

        RegisterSuccessResponse registerSuccessResponse = robertApiClient.register(registerRequest);

        appMobile.decryptRegisterResponse(registerSuccessResponse);

        appMobileMap.put(registerOrderRequest.getCaptchaId(), appMobile);

    }

    public void unregister(String captchaId) {
        log.info("unregister app mobile identified by this captcha id {} ", captchaId);

        AppMobile appMobile = appMobileMap.get(captchaId);

        Objects.requireNonNull(appMobile);

        AuthentifiedRequest authentifiedRequest = appMobile.prepareAuthRequest(0, DigestSaltEnum.UNREGISTER);

        UnregisterRequest unregisterRequest = new UnregisterRequest();
        unregisterRequest.setEbid(authentifiedRequest.getEbid());
        unregisterRequest.setEpochId(authentifiedRequest.getEpochId());
        unregisterRequest.setTime(authentifiedRequest.getTime());
        unregisterRequest.setMac(authentifiedRequest.getMac());

        robertApiClient.unregister(unregisterRequest);

    }

    public void deleteExposureHistory(String captchaId) {
        log.info("delete exposure history of the app mobile identified by this captcha id {} ", captchaId);

        AppMobile appMobile = appMobileMap.get(captchaId);
        Objects.requireNonNull(appMobile);

        AuthentifiedRequest authentifiedRequest = appMobile.prepareAuthRequest(0, DigestSaltEnum.DELETE_HISTORY);

        robertApiClient.deleteExposureHistory(authentifiedRequest);

    }

    public Integer status(String captchaId) {
        log.info("call status of the app mobile identified by this captcha id {} ", captchaId);

        AppMobile appMobile = appMobileMap.get(captchaId);
        Objects.requireNonNull(appMobile);

        AuthentifiedRequest authentifiedRequest = appMobile.prepareAuthRequest(0, DigestSaltEnum.STATUS);

        ExposureStatusRequest exposureStatusRequest = new ExposureStatusRequest();
        exposureStatusRequest.setEbid(authentifiedRequest.getEbid());
        exposureStatusRequest.setEpochId(authentifiedRequest.getEpochId());
        exposureStatusRequest.setTime(authentifiedRequest.getTime());
        exposureStatusRequest.setMac(authentifiedRequest.getMac());

        ExposureStatusResponse exposureStatusResponse = robertApiClient.eSR(exposureStatusRequest);
        appMobile.decryptStatusResponse(exposureStatusResponse);
        return exposureStatusResponse.getRiskLevel();
    }

    public void report(ReportOrderRequest reportOrderRequest) {
        log.info("send report of the app mobile identified by this captcha id {} ", reportOrderRequest.getCaptchaId());

        AppMobile appMobile = appMobileMap.get(reportOrderRequest.getCaptchaId());
        Objects.requireNonNull(appMobile);

        ReportBatchRequest reportBatchRequest = new ReportBatchRequest();
        reportBatchRequest.setToken(reportOrderRequest.getQrCode());
        reportBatchRequest.setContacts(appMobile.getContactsAndRemoveThem());

        robertApiClient.reportBatch(reportBatchRequest);

    }

    public void startHelloMessageExchange(HelloMessageExchangesOrderRequest helloMessageExchangesOrderRequest) {

        AppMobile appMobile = appMobileMap.get(helloMessageExchangesOrderRequest.getCaptchaId());
        Objects.requireNonNull(appMobile);

        List<AppMobile> otherApps = helloMessageExchangesOrderRequest.getCaptchaIdOfOtherApps().stream()
                .map(appMobileMap::get).collect(Collectors.toList());

        appMobile.startHelloMessageExchanges(
                otherApps, Duration.ofSeconds(helloMessageExchangesOrderRequest.getFrequencyInSeconds())
        );

    }

    public void stopHelloMessageExchange(String captchaId) {

        AppMobile appMobile = appMobileMap.get(captchaId);
        Objects.requireNonNull(appMobile);

        appMobile.stopHelloMessageExchanges();

    }

}
