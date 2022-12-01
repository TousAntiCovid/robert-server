package fr.gouv.stopc.e2e.mobileapplication.repository.model;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Date;

@Value
@Builder
@Table("identity")
public class ClientIdentifier {

    @Id
    Long id;

    @Column("ida")
    String idA;

    @Column("key_for_mac")
    String keyForMac;

    @Column("key_for_tuples")
    String keyForTuples;

    @Column("creation_time")
    Date dateCreation;

    @Column("last_update")
    Date dateMaj;
}
