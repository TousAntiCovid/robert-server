package fr.gouv.clea.identification;


import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * Read-only configuration risk for one {type/categ1/categ2} visit 
 */
@AllArgsConstructor
@Getter
public class RiskLevelConfig {

	private int backwardTheshold;

	private int forwardTheshold;
	
	private float backwardRisk;

	private float forwardRisk;
	
}
