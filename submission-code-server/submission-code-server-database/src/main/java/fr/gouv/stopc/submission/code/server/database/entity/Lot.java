package fr.gouv.stopc.submission.code.server.database.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name ="lot_keys")
public class Lot {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
}
