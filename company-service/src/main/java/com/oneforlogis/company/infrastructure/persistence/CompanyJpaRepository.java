package com.oneforlogis.company.infrastructure.persistence;

import com.oneforlogis.company.domain.model.Company;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyJpaRepository extends JpaRepository<Company, UUID> {
}
