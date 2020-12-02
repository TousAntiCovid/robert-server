package fr.gouv.tacw.ws.properties;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "tacw.rest")
public class ScoringProperties {
	private int startOfInterval;
	private int endOfInterval;
	private Map<String,Integer> exposureCountIncrements = new HashMap<String, Integer>();
}
