package fr.gouv.stopc.e2e.external.database.postgresql.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

import java.util.Date;

@Entity
@Table(name = "IDENTITY", indexes = { @Index(name = "IDX_IDA", columnList = "idA") })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
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

    @CreatedDate
    @Column(name = "creation_time", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    protected Date dateCreation;

    @LastModifiedDate
    @Column(name = "last_update")
    @Temporal(TemporalType.TIMESTAMP)
    protected Date dateMaj;
}
