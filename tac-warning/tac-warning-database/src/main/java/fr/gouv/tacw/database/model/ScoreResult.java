package fr.gouv.tacw.database.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public class ScoreResult {
    private final RiskLevel riskLevel;
    private final long score;
    private final long lastContactDate;

    public ScoreResult(RiskLevel riskLevel, long score, long lastContactDate) {
        super();
        this.riskLevel = riskLevel;
        this.score = score;
        this.lastContactDate = lastContactDate;
    }
}
