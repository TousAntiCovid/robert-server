package fr.gouv.tac.systemtest;
// Import classes:
import fr.gouv.tac.tacwarning.ApiClient;
import fr.gouv.tac.tacwarning.Configuration;
import fr.gouv.tac.tacwarning.api.DefaultApi;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class IntegrationAppCLI {
    private static final long TIME_ROUNDING = ServerConfigUtil.getTimeRounding();
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


    }



}