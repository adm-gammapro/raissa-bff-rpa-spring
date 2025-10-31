package com.bff.raissaRpa.domain.repository;

import com.bff.raissaRpa.domain.entity.Key;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KeyRepository extends JpaRepository<Key, Integer> {
    Optional<Key> findByApiKey(String apiKey);
    boolean existsByApiKey(String apiKey);

    @Query("SELECT k FROM Key k WHERE k.active = :active ORDER BY k.createdAt DESC")
    java.util.List<Key> findByActive(@Param("active") Short active);
}
