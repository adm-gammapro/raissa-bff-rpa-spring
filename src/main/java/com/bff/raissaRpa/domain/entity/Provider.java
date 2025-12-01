package com.bff.raissaRpa.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "provider")
public class Provider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 50)
    private String reference;

    @Column(length = 100)
    private String ruta;

    @Column(columnDefinition = "smallint default 0")
    private Short extra;

    @Column(columnDefinition = "smallint default 0")
    private Short detalle;

    @Column(columnDefinition = "smallint default 0")
    private Short historico;

    @Column(columnDefinition = "smallint default 0")
    private Short api;

    @Column(columnDefinition = "smallint default 1")
    private Short active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 25)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 25)
    private String updatedBy;
}
