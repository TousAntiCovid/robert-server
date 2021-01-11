package fr.gouv.tacw.ws.configuration;

import fr.gouv.tacw.database.model.RiskLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VenueConfiguration {
    /** number of Covid+ cases to go over the threshold */
    Integer positiveCasesThreshold;
    /** Venue risk level */
    RiskLevel riskLevel;
}
