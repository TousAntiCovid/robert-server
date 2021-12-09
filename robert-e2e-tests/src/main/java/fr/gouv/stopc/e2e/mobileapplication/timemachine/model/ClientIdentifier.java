package fr.gouv.stopc.e2e.mobileapplication.timemachine.model;

import lombok.*;

import javax.persistence.*;

import java.util.Date;

@Entity
@Table(name = "IDENTITY", indexes = { @Index(name = "IDX_IDA", columnList = "idA") })
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientIdentifier {

    @Id
    @ToString.Exclude
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @Column(name = "idA", unique = true, nullable = false)
    private String idA;

    @ToString.Exclude
    @Column(name = "key_for_mac", updatable = false, nullable = false)
    private String keyForMac;

    @ToString.Exclude
    @Column(name = "key_for_tuples", updatable = false, nullable = false)
    private String keyForTuples;

    @Column(name = "creation_time", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    protected Date dateCreation;

    @Column(name = "last_update")
    @Temporal(TemporalType.TIMESTAMP)
    protected Date dateMaj;
}
