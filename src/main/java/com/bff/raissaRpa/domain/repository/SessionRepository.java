package com.bff.raissaRpa.domain.repository;

import com.bff.raissaRpa.domain.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    /**
     * Busca sesión por transactionId
     */
    Optional<Session> findByTransactionId(String transactionId);
}
