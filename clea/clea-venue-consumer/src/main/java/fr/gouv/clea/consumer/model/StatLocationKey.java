package fr.gouv.clea.consumer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class StatLocationKey implements Serializable {

    @Column(name = "period")
    private Instant period;

    @Column(name = "venue_type")
    private int venueType;

    @Column(name = "venue_category1")
    private int venueCategory1;

    @Column(name = "venue_category2")
    private int venueCategory2;
}
