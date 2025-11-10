package com.oneforlogis.company.infrastructure.persistence;

import com.oneforlogis.company.domain.model.Company;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyJpaRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByIdAndDeletedFalse(UUID id);
    Page<Company> findByDeletedFalse(Pageable pageable);
    Page<Company> findByNameContainingAndDeletedFalse(String name, Pageable pageable);
}
