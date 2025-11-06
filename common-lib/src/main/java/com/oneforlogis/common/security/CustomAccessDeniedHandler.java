package com.oneforlogis.common.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import com.oneforlogis.common.exception.ErrorCode;

@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    public CustomAccessDeniedHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException, ServletException {

        log.warn("✅ [CustomAccessDeniedHandler] invoked — URI={}, message={}",
                request.getRequestURI(), ex.getMessage());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        String json = String.format("{\"isSuccess\":false,\"code\":%d,\"message\":\"%s\"}",
                ErrorCode.FORBIDDEN_ACCESS.getHttpStatus().value(),
                ErrorCode.FORBIDDEN_ACCESS.getMessage());

        response.getWriter().write(json);
    }
}