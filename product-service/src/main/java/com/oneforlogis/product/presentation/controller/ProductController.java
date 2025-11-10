package com.oneforlogis.product.presentation.controller;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.product.application.ProductService;
import com.oneforlogis.product.application.dto.request.ProductCreateRequest;
import com.oneforlogis.product.application.dto.response.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name="products", description = "상품 관리 API")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    // todo: 상품 관리 기본 crud 개발 후 테스트하며 로직 추가 (hub, company 체크 도메인 규칙 등)

    /**
     * 상품 등록
     */
    @Operation(summary = "상품 등록", description = "새로운 상품을 등록합니다. 'MASTER, HUB_MANAGER(담당 허브), COMPANY_MANGER(담당 업체)' 권한이 필요합니다.")
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER') or hasRole('COMPANY_MANAGER')")
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@RequestBody @Valid ProductCreateRequest request){

        var response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

}
