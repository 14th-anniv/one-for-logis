package com.oneforlogis.company.application.dto;

import com.oneforlogis.company.domain.model.Company;
import com.oneforlogis.company.domain.repository.CompanyRepository;
import com.oneforlogis.company.presentation.dto.request.CompanyCreateRequest;
import com.oneforlogis.company.presentation.dto.response.CompanyCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;

    // todo: hub 연결 후 검증 로직 추가

    // 업체 등록
    @Transactional
    public CompanyCreateResponse createCompany(CompanyCreateRequest request){

        Company company = Company.createCompany(request);
        Company savedCompany = companyRepository.save(company);
        return CompanyCreateResponse.from(savedCompany);
    }
}
