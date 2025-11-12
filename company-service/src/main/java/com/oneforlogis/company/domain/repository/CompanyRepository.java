package com.oneforlogis.company.domain.repository;

import com.oneforlogis.company.domain.model.Company;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CompanyRepository {

    Company save(Company company);
    Optional<Company> findByIdAndDeletedFalse(UUID id);
    Page<Company> findByDeletedFalse(Pageable pageable);
    Page<Company> findByNameContainingAndDeletedFalse(String keyword, Pageable pageable);
}
