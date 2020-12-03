package fr.gouv.tac.systemtest;

import fr.gouv.tac.tacwarning.auth.HttpBearerAuth;
import fr.gouv.tac.robert.model.*;
import fr.gouv.tac.tacwarning.ApiException;
import fr.gouv.tac.tacwarning.model.ExposureStatusRequest;
import fr.gouv.tac.tacwarning.model.ExposureStatusResponse;
import fr.gouv.tac.tacwarning.model.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This class represents a view of a visitor device (phone)
 * it modelizes its status  (scanned QRCodes, robert status, ...)   in order to capture steps state changes.
 */
public class Visitor {

	private static Logger logger = LoggerFactory.getLogger(Visitor.class);
	
    private static final int MAX_SALT = 1000;
    private static Random random = new Random();
    private String name = "";
    private List<Visit> visitList = new ArrayList<Visit>();
    private List<VisitToken> tokens = new ArrayList<VisitToken>();
    private fr.gouv.tac.robert.model.RegisterRequest registerRequest = null;
    
    private RegisterSuccessResponse lastRegisterSuccessResponse = null;
    private ExposureStatusResponse lastExposureStatusResponse = null;
    private ReportResponse lastTACWarningReportResponse = null;

    private Boolean covidStatus = false;



    private String outcome ;
    private String jwt;

    public List<VisitToken> getTokens() {
        return this.tokens;
    }

    public void setTokens(List<VisitToken> tokens) {
        this.tokens = tokens;
    }


	public Visitor() {
    }

    public Visitor(String name, String place, String time, String status) {
        this.name = name;

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

    public RegisterSuccessResponse getLastRegisterSuccessResponse() {
		return lastRegisterSuccessResponse;
	}

	public void setLastRegisterSuccessResponse(RegisterSuccessResponse lastRegisterSuccessResponse) {
		this.lastRegisterSuccessResponse = lastRegisterSuccessResponse;
	}

	public ExposureStatusResponse getLastExposureStatusResponse() {
		return lastExposureStatusResponse;
	}

	public void setLastExposureStatusResponse(ExposureStatusResponse lastExposureStatusResponse) {
		this.lastExposureStatusResponse = lastExposureStatusResponse;
	}

	public ReportResponse getLastTACWarningReportResponse() {
		return lastTACWarningReportResponse;
	}

	public void setLastTACWarningReportResponse(ReportResponse lastTACWarningReportResponse) {
		this.lastTACWarningReportResponse = lastTACWarningReportResponse;
	}

	public Boolean sendTacWarningStatus(fr.gouv.tac.tacwarning.api.DefaultApi apiInstance) {
        Boolean outcome = null;
        ExposureStatusRequest exposureStatusRequest = new ExposureStatusRequest();
        for (VisitToken token : tokens) {
            exposureStatusRequest.addVisitTokensItem(token);
        }
        try {
            ExposureStatusResponse result = apiInstance.eSR(exposureStatusRequest);
            this.setLastExposureStatusResponse(result);
            outcome = result.getAtRisk();
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return outcome;
    }
	

    public Boolean sendTacReport(fr.gouv.tac.robert.api.DefaultApi apiInstance) {
        Boolean outcome = null;
        fr.gouv.tac.robert.model.
        ReportBatchRequest reportBatchRequest = new fr.gouv.tac.robert.model.ReportBatchRequest();
        for (Visit visit : visitList) {
            reportBatchRequest.token("string");
            List<Contact> contacts = new ArrayList<Contact>();
            reportBatchRequest.setContacts(contacts);
        }
        try {
            ReportBatchResponse reportBatchResponse = apiInstance.reportBatch(reportBatchRequest);
            String message = reportBatchResponse.getMessage();
            outcome = reportBatchResponse.getSuccess();
            this.setJwt(reportBatchResponse.getToken());
        } catch (fr.gouv.tac.robert.ApiException e) {
        	logger.error("Exception when calling RobertDefaultApi#reportBatch", e);
        	logger.error("Status code: " + e.getCode());
        	logger.error("Reason: " + e.getResponseBody());
        	logger.error("Response headers: " + e.getResponseHeaders());
        }
        return outcome;
    }

    private void setJwt(String token) {
        jwt = token;
    }

    public String getOutcome() {
        return this.outcome;
    }

    public void setOutcome(final String outcome) {
        this.outcome = outcome;
    }

    public Boolean sendTacWarningReport(fr.gouv.tac.tacwarning.api.DefaultApi apiInstance) {
        Boolean outcome = null;
        ReportRequest reportRequest = new fr.gouv.tac.tacwarning.model.ReportRequest();
        for (Visit visit : visitList) {
            reportRequest.addVisitsItem(visit);
            ((HttpBearerAuth)(apiInstance.getApiClient().getAuthentication("bearerAuth"))).setBearerToken(this.jwt);
        }
        try {
            ReportResponse response = apiInstance.report(reportRequest);
            this.setLastTACWarningReportResponse(response);
            outcome = response.getSuccess();
        } catch (ApiException e) {
        	logger.error("Exception when calling TACWarningDefaultApi#report", e);
        	logger.error("Status code: " + e.getCode());
        	logger.error("Reason: " + e.getResponseBody());
        	logger.error("Response headers: " + e.getResponseHeaders());
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