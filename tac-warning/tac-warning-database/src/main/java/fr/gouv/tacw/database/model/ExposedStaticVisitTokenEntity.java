package fr.gouv.tacw.database.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.GenericGenerator;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@Entity
@Table(name="EXPOSED_STATIC_VISIT_TOKEN")
public class ExposedStaticVisitTokenEntity {
    @Id
    @GeneratedValue(generator = "generator")
    @GenericGenerator(name = "generator", strategy = "increment")	
    @ToString.Exclude
    private Long id;
    
    @NotNull
	private String token;

    @NotNull
    private long timestamp;
    
    public ExposedStaticVisitTokenEntity(long timestamp, String token) {
    	this.timestamp = timestamp;
    	this.token = token;
    }
}
