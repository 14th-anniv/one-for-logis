package com.oneforlogis.company.domain.exception;

import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;

public class AlreadyDeletedCompanyException extends CustomException {

    public AlreadyDeletedCompanyException(ErrorCode errorCode) {
        super(errorCode);
    }
}
