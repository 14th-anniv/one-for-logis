package com.oneforlogis.company.domain.model;

import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.company.domain.exception.InvalidCompanyTypeException;

public enum CompanyType {
    SUPPLIER, // 생산 업체
    RECEIVER;  // 수령 업체

    public static CompanyType from(String value) {
        try {
            return CompanyType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidCompanyTypeException(ErrorCode.COMPANY_INVALID_TYPE);
        }
    }
}
