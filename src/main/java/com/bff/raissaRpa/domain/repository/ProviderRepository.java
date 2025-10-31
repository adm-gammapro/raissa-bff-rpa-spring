package com.bff.raissaRpa.domain.repository;

import com.bff.raissaRpa.domain.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Integer> {
    Optional<Provider> findByReferenceAndActive(String reference, Short active);
}
