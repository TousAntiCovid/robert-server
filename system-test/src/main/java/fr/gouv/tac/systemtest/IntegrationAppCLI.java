package fr.gouv.tac.systemtest;
// Import classes:
import fr.gouv.tac.tacwarning.ApiClient;
import fr.gouv.tac.tacwarning.ApiException;
import fr.gouv.tac.tacwarning.Configuration;
import fr.gouv.tac.tacwarning.api.DefaultApi;
import fr.gouv.tac.tacwarning.model.ExposureStatusRequest;
import fr.gouv.tac.tacwarning.model.ExposureStatusResponse;
import fr.gouv.tac.tacwarning.model.VisitToken;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class IntegrationAppCLI {
    private static final int TIME_ROUNDING = 900;
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        String tacServerPath = System.getenv("TACW_BASE_URL");
        if (tacServerPath == null || tacServerPath.isEmpty()){
            tacServerPath = "http://localhost";
        }
        tacServerPath = tacServerPath+ "/v1";

        defaultClient.setBasePath(tacServerPath);

        DefaultApi apiInstance = new DefaultApi(defaultClient);

        LocalDateTime dateTime=LocalDateTime.of(1900,1,1,0,0,0);
        LocalDateTime dateTime2=LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(1);
        Long timestamp=java.time.Duration.between(dateTime,dateTime2).getSeconds();
        timestamp=timestamp-(timestamp % TIME_ROUNDING);

        Visitor hugo = new Visitor();
        Visitor stacy = new Visitor();
        Place restaurant = new Place();
        Place cafe = new Place();
        hugo.addVisit(restaurant.getQrCode(),TimeUtil.yesterdayAt(12));
        List<Long> list = TimeUtil.everyDayAt(13);
        hugo.addMultipleVisit(cafe.getQrCode(), list);

        list = TimeUtil.everyDayAt(12);
        stacy.addMultipleVisit(cafe.getQrCode(), list);

        sendTacWarningStatus(hugo.getTokens(),apiInstance);
        sendTacWarningStatus(stacy.getTokens(),apiInstance);

    }

    private static void sendTacWarningStatus(List<VisitToken> tokens,DefaultApi apiInstance) {

        ExposureStatusRequest exposureStatusRequest = new ExposureStatusRequest();
        exposureStatusRequest.setVisitTokens(tokens);
        try {
            ExposureStatusResponse result = apiInstance.eSR(exposureStatusRequest);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling DefaultApi#eSR");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }

}