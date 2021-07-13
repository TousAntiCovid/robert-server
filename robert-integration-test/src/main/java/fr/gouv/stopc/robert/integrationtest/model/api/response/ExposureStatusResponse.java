package fr.gouv.stopc.robert.integrationtest.model.api.response;

import fr.gouv.stopc.robert.integrationtest.model.api.common.ConfigurationProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ExposureStatusResponse {
    public static final String JSON_PROPERTY_RISK_LEVEL = "riskLevel";
    private Integer riskLevel;

    public static final String JSON_PROPERTY_LAST_CONTACT_DATE = "lastContactDate";
    private String lastContactDate;

    public static final String JSON_PROPERTY_LAST_RISK_SCORING_DATE = "lastRiskScoringDate";
    private String lastRiskScoringDate;

    public static final String JSON_PROPERTY_MESSAGE = "message";
    private String message;

    public static final String JSON_PROPERTY_TUPLES = "tuples";
    private byte[] tuples;

    public static final String JSON_PROPERTY_DECLARATION_TOKEN = "declarationToken";
    private String declarationToken;

    public static final String JSON_PROPERTY_ANALYTICS_TOKEN = "analyticsToken";
    private String analyticsToken;

    public static final String JSON_PROPERTY_CONFIG = "config";
    private List<ConfigurationProperty> config = new ArrayList<>();

}
