package fr.gouv.tac.mobile.emulator.controller;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.tac.mobile.emulator.api.EmulatorApi;
import fr.gouv.tac.mobile.emulator.api.model.HelloMessageExchangesOrderRequest;
import fr.gouv.tac.mobile.emulator.api.model.RegisterOrderRequest;
import fr.gouv.tac.mobile.emulator.api.model.ReportOrderRequest;
import fr.gouv.tac.mobile.emulator.service.EmulatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping(path = "/api/v1/emulator")
@RequiredArgsConstructor()
@Slf4j
public class EmulatorController implements EmulatorApi {

    private final EmulatorService emulatorService;

    @Override
    public ResponseEntity<Void> register(RegisterOrderRequest registerOrderRequest) {
        log.info("register {} ", registerOrderRequest.toString());

        try {
            emulatorService.register(registerOrderRequest);
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | RobertServerCryptoException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> unregister(String captchaId) {
        log.info("unregister app mobile identified by this captcha id {} ", captchaId);

        emulatorService.unregister(captchaId);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deleteExposureHistory(String captchaId) {
        log.info("delete exposure history for the app mobile identified by this captcha id {} ", captchaId);

        emulatorService.deleteExposureHistory(captchaId);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> status(String captchaId) {
        log.info("status for the app mobile identified by this captcha id {} ", captchaId);

        emulatorService.status(captchaId);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> report(ReportOrderRequest reportOrderRequest) {
        log.info("report for the app mobile identified by this captcha id {} ", reportOrderRequest.getCaptchaId());

        emulatorService.report(reportOrderRequest);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> startHelloMessageExchanges(HelloMessageExchangesOrderRequest helloMessageExchangesOrderRequest) {
        log.info("start hello message exchanges managed by app mobile with captcha  : {}", helloMessageExchangesOrderRequest.getCaptchaId());

        emulatorService.startHelloMessageExchange(helloMessageExchangesOrderRequest);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> stopHelloMessageExchanges(String captchaId) {
        log.info("stop hello message exchanges for the app mobile identified by this captcha id {} ", captchaId);

        emulatorService.stopHelloMessageExchange(captchaId);

        return ResponseEntity.ok().build();

    }
}
