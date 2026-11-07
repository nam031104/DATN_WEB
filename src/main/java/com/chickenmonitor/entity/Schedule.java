package com.chickenmonitor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;

@Entity
@Table(name = "schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private Actor actor;

    @Column(name = "execution_time", nullable = false)
    private LocalTime executionTime; // Ánh xạ với kiểu TIME trong DB

    @Column(name = "status")
    private Integer status;

    @Column(name = "mode")
    private Integer mode;

    @Column(name = "is_executed")
    private Boolean isExecuted; // Dùng Boolean cho kiểu dữ liệu logic (tinyint(1) hoặc bit)

    @Column(name = "last_executed_date")
    private LocalDate lastExecutedDate;

    @Column(name = "note", length = 255)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
