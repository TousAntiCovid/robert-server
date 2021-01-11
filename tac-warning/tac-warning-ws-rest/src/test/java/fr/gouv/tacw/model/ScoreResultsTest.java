package fr.gouv.tacw.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import fr.gouv.tacw.database.model.RiskLevel;
import fr.gouv.tacw.database.model.ScoreResult;

public class ScoreResultsTest {

    @Test
    public void testMergeOfTwoEmptyScoreResultsGivesScoreResults() {
        ScoreResults result = new ScoreResults().merge(new ScoreResults());
        
        assertThat(result.getScores()).isEmpty();
    }

    @Test
    public void testMergeAnEmptyScoreResultsWithAScoreResultsGivesScoreResults() {
        ScoreResults otherScores = this.scoresWithHighAndLowLevel();
        ScoreResults result = new ScoreResults().merge(otherScores);
        
        assertThat(result.getScores()).containsExactlyInAnyOrderElementsOf(otherScores.getScores());
    }
    
    @Test
    public void testMergeScoreResultsHavingOneRiskLevelWithAScoreResultsHavingAnotherRiskLevelGivesScoreResultsWithTwoRiskLevels() {
        ScoreResults 
            scores = new ScoreResults( Arrays.asList(this.highLevelScore()) ),
            otherScores = new ScoreResults( Arrays.asList(this.lowLevelScore()) );
            
        ScoreResults result = scores.merge(otherScores);
        
        assertThat(result.getScores())
            .containsAll(scores.getScores())
            .containsAll(otherScores.getScores());
        assertThat(result.getScores().size()).isEqualTo(2);
    }
    
    @Test
    public void testMergeScoreResultsWithAScoreResultsHavingSameRiskLevelGivesScoreResultsWithMergedRiskLevels() {
        ScoreResults 
            scores = new ScoreResults( Arrays.asList(this.lowLevelScore2()) ),
            otherScores = this.scoresWithHighAndLowLevel();
            
        ScoreResults result = scores.merge(otherScores);
        
        assertThat(result.getScores())
            .contains(this.highLevelScore())
            .contains(new ScoreResult(RiskLevel.LOW, 200, 19540));
        assertThat(result.getScores().size()).isEqualTo(2);
    }
    
    @Test
    public void testMaxRiskLevelReachedWhenNoScoreResultThenMaxRiskLevelIsNone() {
        RiskLevel riskLevel = new ScoreResults().getMaxRiskLevelReached(100);
        
        assertThat(riskLevel).isEqualTo(RiskLevel.NONE);
    }
    
    @Test
    public void testMaxRiskLevelReachedWhenOneScoreResultThenMaxRiskLevelIsRiskLevelInScoreResult() {
        ScoreResults scores = new ScoreResults( Arrays.asList(this.lowLevelScore()) );
        
        RiskLevel riskLevel = scores.getMaxRiskLevelReached(100);
        
        assertThat(riskLevel).isEqualTo(RiskLevel.LOW);
    }
    
    @Test
    public void testMaxRiskLevelReachedWhenNoRiskLevelOverScoreThresholdThenMaxRiskLevelIsRiskLevelNone() {
        ScoreResults scores = new ScoreResults( Arrays.asList(this.lowLevelScore()) );
        
        RiskLevel riskLevel = scores.getMaxRiskLevelReached(1000);
        
        assertThat(riskLevel).isEqualTo(RiskLevel.NONE);
    }
    
    @Test
    public void testMaxRiskLevelReachedWhenManyScoreResults() {
        ScoreResults scores = this.scoresWithHighAndLowLevel();
        
        RiskLevel riskLevel = scores.getMaxRiskLevelReached(100);
        
        assertThat(riskLevel).isEqualTo(RiskLevel.HIGH);
    }
    
    @Test
    public void testMaxRiskLevelReachedWhenManyScoreResultsAndOnlySomeOverScoreThreshold() {
        ScoreResults scores =  new ScoreResults( Arrays.asList(
                new ScoreResult(RiskLevel.HIGH, 100, 18000), 
                new ScoreResult(RiskLevel.LOW, 300, 18000), 
                new ScoreResult(RiskLevel.LOW, 100, 18000), 
                new ScoreResult(RiskLevel.HIGH, 100, 18000),
                new ScoreResult(RiskLevel.LOW, 200, 18000) ));
        scores = new ScoreResults().merge(scores);
        
        RiskLevel riskLevel = scores.getMaxRiskLevelReached(600);
        
        assertThat(riskLevel).isEqualTo(RiskLevel.LOW);
    }
    
    @Test
    public void testMaxRiskLevelReachedWhenNoScoreOverThresholdThenGetRiskLevelNone() {
        ScoreResults scores = this.scoresWithHighAndLowLevel();
        
        RiskLevel riskLevel = scores.getMaxRiskLevelReached(1000);
        
        assertThat(riskLevel).isEqualTo(RiskLevel.NONE);
    }
    
    protected ScoreResults scoresWithHighAndLowLevel() {
        return new ScoreResults( Arrays.asList(this.highLevelScore(), this.lowLevelScore()) );
    }
    
    protected ScoreResult highLevelScore() {
        return new ScoreResult(RiskLevel.HIGH, 100, 18000);
    }

    protected ScoreResult lowLevelScore() {
        return new ScoreResult(RiskLevel.LOW, 100, 19000);
    }

    protected ScoreResult lowLevelScore2() {
        return new ScoreResult(RiskLevel.LOW, 100, 19540);
    }
}
