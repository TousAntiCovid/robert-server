package fr.gouv.tacw.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import fr.gouv.tacw.database.model.RiskLevel;
import fr.gouv.tacw.database.model.ScoreResult;
import fr.gouv.tacw.database.utils.TimeUtils;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class ScoreResults {
    private final Collection<ScoreResult> scores;

    public ScoreResults(Collection<ScoreResult> scores) {
        super();
        this.scores = scores;
    }

    public ScoreResults() {
        this.scores = new ArrayList<ScoreResult>();
    }
    
    public ScoreResults merge(ScoreResults otherScoreResults) {
        Map<Integer, ScoreResult> scores = new HashMap<Integer,ScoreResult>();
        this.getScores().stream()
            .forEach(score -> scores.put(score.getRiskLevel().getValue(), score));
        otherScoreResults.getScores().stream()
            .forEach(score -> {
                int key = score.getRiskLevel().getValue();
                if (scores.containsKey(key)) {
                    scores.put(key, this.merge(score, scores.get(key)));
                } else {
                    scores.put(key, score);
                }
            });
        return new ScoreResults(scores.values());
    }

    protected ScoreResult merge(ScoreResult score1, ScoreResult score2) {
        return new ScoreResult(
                score2.getRiskLevel(),
                score2.getScore() + score1.getScore(),
                Math.max(score2.getLastContactDate(), score1.getLastContactDate()));
    }

    public RiskLevel getMaxRiskLevelReached(int threshold) {
        return this.getScoreWithMaxRiskLevelReached(threshold).getRiskLevel();
    }
    
    public ScoreResult getScoreWithMaxRiskLevelReached(int threshold) {
        Optional<ScoreResult> maxScore = this.scores.stream()
                .filter(result -> result.getScore() >= threshold)
                .max( Comparator.comparing(ScoreResult::getRiskLevel) );
        if (maxScore.isPresent()) {
            ScoreResult maxScoreResult = maxScore.get();
            return new ScoreResult(maxScoreResult.getRiskLevel(), maxScoreResult.getScore(),
                    TimeUtils.dayTruncatedTimestamp(maxScoreResult.getLastContactDate()));
        } else {
            return new ScoreResult(RiskLevel.NONE, 0, -1);
        }
    }
}
