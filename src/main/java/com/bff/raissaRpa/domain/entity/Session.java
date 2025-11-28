package com.bff.raissaRpa.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consumer_ip", length = 15)
    private String consumerIp;

    @Column(name = "consumer_useragent", length = 50)
    private String consumerUseragent;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "token", length = 500)
    private String token;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "active")
    private Integer active = 1;

    @Column(name = "expires")
    private LocalDateTime expires;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "disabled_at")
    private LocalDateTime disabledAt;

    @PrePersist
    protected void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return active == 1 && (expires == null || expires.isAfter(LocalDateTime.now()));
    }
}
