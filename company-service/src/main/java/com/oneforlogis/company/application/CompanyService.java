package com.oneforlogis.company.application;

import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.company.application.dto.request.CompanyCreateRequest;
import com.oneforlogis.company.application.dto.request.CompanyUpdateRequest;
import com.oneforlogis.company.application.dto.response.CompanyCreateResponse;
import com.oneforlogis.company.application.dto.response.CompanyUpdateResponse;
import com.oneforlogis.company.domain.model.Company;
import com.oneforlogis.company.domain.model.CompanyType;
import com.oneforlogis.company.domain.repository.CompanyRepository;
import java.util.UUID;
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

        Company company = Company.createCompany(
                request.name(),
                CompanyType.from(request.type()),
                request.hubId(),
                request.address()
        );

        Company savedCompany = companyRepository.save(company);
        return CompanyCreateResponse.from(savedCompany);
    }

    // 업체 수정
    @Transactional
    public CompanyUpdateResponse updateCompany(UUID companyId, CompanyUpdateRequest request){

        Company company = getCompanyById(companyId);

        if (request.name() != null && !request.name().isBlank()) {
            company.updateName(request.name());
        }
        if (request.type() != null && !request.type().isBlank()) {
            company.updateType(CompanyType.from(request.type()));
        }
        if (request.address() != null) {
            company.updateAddress(request.address());
        }

        return CompanyUpdateResponse.from(company);
    }

    // 업체 삭제
    @Transactional
    public void deleteCompany(UUID companyId, String userName){
        Company company = getCompanyById(companyId);
        log.info("service - del company userName: {}", userName);
        company.deleteCompany(userName);
    }



    /**
     * 중복되는 코드 헬퍼 메서드
     */

    // 업체 엔티티 조회
    public Company getCompanyById(UUID companyId){
        return companyRepository.findByIdAndDeletedFalse(companyId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));
    }
}
