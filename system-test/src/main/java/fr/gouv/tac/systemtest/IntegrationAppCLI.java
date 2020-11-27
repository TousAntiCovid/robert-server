package fr.gouv.tac.systemtest;
// Import classes:
import fr.gouv.tac.tacwarning.ApiClient;
import fr.gouv.tac.tacwarning.ApiException;
import fr.gouv.tac.tacwarning.Configuration;
import fr.gouv.tac.tacwarning.api.DefaultApi;
import fr.gouv.tac.tacwarning.model.*;

public class IntegrationAppCLI {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost/api/tac-warning/v1");

        DefaultApi apiInstance = new DefaultApi(defaultClient);
        ExposureStatusRequest exposureStatusRequest = new ExposureStatusRequest(); // ExposureStatusRequest |
        VisitToken visitToken = new VisitToken();
        visitToken.setType(VisitToken.TypeEnum.STATIC);
        visitToken.setPayload("7625d7d4a5b45f98f95ef25ed8951da566609a60");
        exposureStatusRequest.addVisitTokensItem(visitToken);
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