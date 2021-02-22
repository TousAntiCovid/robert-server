package fr.gouv.stopc.robertserver.ws.dto.declaration;

import fr.gouv.stopc.robertserver.ws.dto.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class GenerateDeclarationTokenRequest {

    private String technicalApplicationIdentifier;

    private RiskLevel riskLevel;

    private long lastStatusRequestTimestamp;

    private long lastContactDateTimestamp;

    private int latestRiskEpoch;

    @Override
    public String toString() {
        return "GenerateDeclarationTokenRequest{" +
                "technicalApplicationIdentifier='" + technicalApplicationIdentifier + '\'' +
                ", riskLevel=" + riskLevel +
                ", lastStatusRequestTimestamp=" + lastStatusRequestTimestamp +
                ", lastContactDateTimestamp=" + lastContactDateTimestamp +
                ", latestRiskEpoch=" + latestRiskEpoch +
                '}';
    }
}
