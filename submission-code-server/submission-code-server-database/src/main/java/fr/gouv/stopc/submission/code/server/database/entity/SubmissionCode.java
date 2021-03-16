package fr.gouv.stopc.submission.code.server.database.entity;

import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.time.OffsetDateTime;


@Data
@Entity
@Table(name ="submission_code")
public class SubmissionCode {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Lot lotkey;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "type_code", nullable = false)
    private String type;

    @Column(name = "date_end_validity", nullable = false)
    private OffsetDateTime dateEndValidity;

    @Column(name = "date_available", nullable = false)
    private OffsetDateTime dateAvailable;

    @Column(name = "date_use")
    private OffsetDateTime dateUse;

    @Column(name = "date_generation", nullable = false)
    private OffsetDateTime dateGeneration;

    @Column(name = "used", nullable = false)
    private Boolean used;

}
