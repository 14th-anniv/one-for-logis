package com.oneforlogis.company.domain.repository;

import com.oneforlogis.company.domain.model.Company;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository {

    Company save(Company company);
    Optional<Company> findByIdAndDeletedFalse(UUID id);
}
