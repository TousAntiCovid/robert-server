package fr.gouv.stopc.robert.integrationtest.service;

import fr.gouv.stopc.robert.integrationtest.model.AppMobile;
import fr.gouv.stopc.robert.integrationtest.model.api.request.HelloMessageExchangesOrderRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TestService {

    public HelloMessageExchangesOrderRequest createHelloMessageExchangesOrderRequest(String captchaId,
                                                                                     List<String> captchaIdOfOtherApps) {
        HelloMessageExchangesOrderRequest helloMEOR = new HelloMessageExchangesOrderRequest();
        helloMEOR.setCaptchaId(captchaId);
        helloMEOR.setCaptchaIdOfOtherApps(captchaIdOfOtherApps);
        helloMEOR.setFrequencyInSeconds(10); // Quelle veleur ?
        return helloMEOR;
    }

    public void startHelloMessageExchange(AppMobile appMobile,
                                          List<String> limitedAppMobileIds,
                                          Map<String, AppMobile> appMobileMap) {
        Objects.requireNonNull(appMobile);

        HelloMessageExchangesOrderRequest helloMessageReq = this.createHelloMessageExchangesOrderRequest(appMobile
                        .getCaptchaId(), limitedAppMobileIds);

        List<AppMobile> otherApps = helloMessageReq.getCaptchaIdOfOtherApps().stream().map(appMobileMap::get)
                .collect(Collectors.toList());

        // TO CHECK : erreur de nommage ? ici sec et startHelloMessageExchanges -> delayInMin ?
        appMobile.startHelloMessageExchanges(otherApps, Duration.ofSeconds(helloMessageReq.getFrequencyInSeconds()));
    }

    public void stopHelloMessageExchanges(AppMobile appMobile) {
        Objects.requireNonNull(appMobile);
        appMobile.stopHelloMessageExchanges();

    }
}
