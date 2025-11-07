package com.oneforlogis.company.domain.exception;

import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;

public class InvalidCompanyTypeException extends CustomException {

    public InvalidCompanyTypeException(ErrorCode errorCode) {
        super(errorCode);
    }
}
