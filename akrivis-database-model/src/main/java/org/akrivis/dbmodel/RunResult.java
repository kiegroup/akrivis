package org.akrivis.dbmodel;

import jakarta.persistence.*;
import org.hibernate.Length;
import org.hibernate.annotations.ColumnTransformer;
import java.time.Instant;

@Entity
@Table(name = "run_result")
public class RunResult {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "run_result_id_seq")
    @SequenceGenerator(name = "run_result_id_seq", sequenceName = "run_result_id_seq", allocationSize = 1)
    public long id;

    public String status;

    @Column(name = "measure_name")
    public String measureName;

    @Column(name = "measure_value", length = Length.LONG32)
    @ColumnTransformer(write = "?::jsonb")
    public String measureValue;

    @Column(name = "max_value")
    public Integer maxValue;

    @Column(name = "run_time")
    public Instant runTime;

    @Column(name = "card_data", length = Length.LONG32)
    @ColumnTransformer(write = "?::jsonb")
    public String cardData;

    @Column(name = "configuration_data", length = Length.LONG32)
    @ColumnTransformer(write = "?::jsonb")
    public String configurationData;

    @ManyToOne
    @JoinColumn(name = "card_id", nullable = false)
    public Card card;
}
