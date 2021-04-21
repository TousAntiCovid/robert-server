package fr.gouv.clea.consumer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "STAT_LOCATION")
public class StatLocation {

    @EmbeddedId
    private StatLocationKey statLocationKey;

    @Column(name = "backward_visits")
    private long backwardVisits;

    @Column(name = "forward_visits")
    private long forwardVisits;
}
