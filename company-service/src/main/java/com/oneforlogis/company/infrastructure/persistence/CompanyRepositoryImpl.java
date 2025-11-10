package com.oneforlogis.company.infrastructure.persistence;

import com.oneforlogis.company.domain.model.Company;
import com.oneforlogis.company.domain.repository.CompanyRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class CompanyRepositoryImpl implements CompanyRepository {

    private final CompanyJpaRepository companyJpaRepository;

    @Override
    public Company save(Company company) {
        return companyJpaRepository.save(company);
    }

    @Override
    public Optional<Company> findByIdAndDeletedFalse(UUID id){
        return companyJpaRepository.findByIdAndDeletedFalse(id);
    }

    @Override
    public Page<Company> findByDeletedFalse(Pageable pageable) {
        return companyJpaRepository.findByDeletedFalse(pageable);
    }

    @Override
    public Page<Company> findByNameContainingAndDeletedFalse(String keyword, Pageable pageable) {
        return companyJpaRepository.findByNameContainingAndDeletedFalse(keyword, pageable);
    }
}
