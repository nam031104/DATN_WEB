package com.chickenmonitor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "actor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Actor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "actor_id")
    private Long actorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "external_code", length = 100)
    private String externalCode;

    @Column(name = "type_actor", nullable = false, length = 50)
    private String typeActor;

    @Column(name = "mode")
    private Integer mode; // tinyint trong DB tương ứng với Integer hoặc Byte trong Java

    @Column(name = "status")
    private Integer status;

    @Column(name = "threshold_min")
    private Double thresholdMin;

    @Column(name = "threshold_max")
    private Double thresholdMax;

    @Column(name = "source_sensor_code", length = 100)
    private String sourceSensorCode;

    @OneToMany(mappedBy = "actor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Schedule> schedules;
}