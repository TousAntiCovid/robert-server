package fr.gouv.tacw.database.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@Entity
@Table(name="EXPOSED_STATIC_VISIT_TOKEN")
public class ExposedStaticVisitTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
	@ToString.Exclude
    private Long id;
    
    @NotNull
	String token;
    
    public ExposedStaticVisitTokenEntity(String token) {
    	this.token = token;
    }
}
