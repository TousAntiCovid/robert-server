package fr.gouv.tac.systemtest;

import fr.gouv.tac.robert.model.PushInfo;
import fr.gouv.tac.robert.model.RegisterRequest;
import fr.gouv.tac.robert.model.RegisterSuccessResponse;
import fr.gouv.tac.tacwarning.ApiException;
import fr.gouv.tac.tacwarning.model.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Visitor {
    private static final int MAX_SALT = 1000;
    private static Random random = new Random();
    private String name = "";
    private List<Visit> visitList = new ArrayList<Visit>();
    private List<VisitToken> tokens = new ArrayList<VisitToken>();
    private fr.gouv.tac.robert.model.RegisterRequest registerRequest = null;

    private Boolean covidStatus = false;

    public List<VisitToken> getTokens() {
        return this.tokens;
    }

    public void setTokens(List<VisitToken> tokens) {
        this.tokens = tokens;
    }

    public Visitor() {
    }

    public Visitor(String name, String place, String time, String status) {
        name = name;

        Visit visit = new MyVisit(place);
        Long time2 = new PrettyTimeParser().parse(time).get(0).getTime();
        visit.setTimestamp(Long.toString(time2));

    }

    public void addVisit(QRCode qrCode, Long timestamp) {
        Visit visit = new MyVisit("unknown");
        visit.setTimestamp(Long.toString(timestamp));
        visit.setQrCode(qrCode);
        this.visitList.add(visit);
        VisitToken visitToken = new VisitToken();
        visitToken.setType(VisitToken.TypeEnum.STATIC);
        visitToken.setPayload(DigestUtils.sha256Hex(random.nextInt(MAX_SALT) + qrCode.getUuid()).toString());
        visitToken.setTimestamp(visit.getTimestamp());
        this.tokens.add(visitToken);
    }

    public void addMultipleVisit(QRCode qrCode, List<Long> list) {
        for (Long temp : list) {
            //          addVisit(qrCode,temp);
        }
    }

    public Visitor(String name) {
        this.setName(name);
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Boolean getCovidStatus() {
        return this.covidStatus;
    }
    public void setCovidStatus(final String covidStatus) {
        if (covidStatus.equals("positive"))
            this.covidStatus = true;
        else
            this.covidStatus = false;
    }
    public void setCovidStatus(final Boolean covidStatus) {
        this.covidStatus = covidStatus;
    }

    public Boolean sendTacWarningStatus(fr.gouv.tac.tacwarning.api.DefaultApi apiInstance) {
        Boolean outcome = null;
        ExposureStatusRequest exposureStatusRequest = new ExposureStatusRequest();
        for (VisitToken token : tokens) {
            exposureStatusRequest.addVisitTokensItem(token);
        }
        try {
            ExposureStatusResponse result = apiInstance.eSR(exposureStatusRequest);
            outcome = result.getAtRisk();
        } catch (ApiException e) {
            System.err.println("Exception when calling DefaultApi#eSR");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
        return outcome;
    }

    public RegisterRequest getRegisterRequest() {
        if (registerRequest == null){
            registerRequest = new RegisterRequest();
            registerRequest.setCaptcha("string");
            registerRequest.setCaptchaId("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            registerRequest.setClientPublicECDHKey("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEB+Q03HmTHYPpHUs3UZIcY0robfRuP0zIVwItwseq8JMCl8W9yCuVRyFGTqL7VqnhZN1tQqS4nwbEW4FSK/JLbg==");
            PushInfo pushInfo = new PushInfo();
            pushInfo.setLocale("fr");
            pushInfo.setTimezone("Europe/Paris");
            pushInfo.setToken("string");
            registerRequest.setPushInfo(pushInfo);
            }
        return this.registerRequest;
    }



    public String tacRobertRegister(fr.gouv.tac.robert.api.DefaultApi apiInstance) {
        String message = null;
        try {
            RegisterSuccessResponse registerSuccessResponse =  apiInstance.register(getRegisterRequest());
            message = registerSuccessResponse.getMessage();
        } catch (fr.gouv.tac.robert.ApiException e) {
            e.printStackTrace();
        }
        return message;
    }
}