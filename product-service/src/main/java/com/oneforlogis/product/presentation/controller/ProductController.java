package com.oneforlogis.product.presentation.controller;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.common.api.PageResponse;
import com.oneforlogis.common.security.UserPrincipal;
import com.oneforlogis.product.application.ProductService;
import com.oneforlogis.product.application.dto.request.ProductCreateRequest;
import com.oneforlogis.product.application.dto.request.ProductUpdateRequest;
import com.oneforlogis.product.application.dto.response.ProductDetailResponse;
import com.oneforlogis.product.application.dto.response.ProductResponse;
import com.oneforlogis.product.application.dto.response.ProductSearchResponse;
import com.oneforlogis.product.application.dto.response.ProductUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /**
     * 상품 정보 수정
     */
    @Operation(summary = "상품 수정", description = "상품 정보를 수정합니다. 'MASTER, HUB_MANAGER(담당 허브), COMPANY_MANAGER(담당 업체)' 권한이 필요합니다.")
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER') or hasRole('COMPANY_MANAGER')")
    @PatchMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductUpdateResponse>> updateProduct(@PathVariable UUID productId,
            @RequestBody @Valid ProductUpdateRequest request){

        var response = productService.updateProduct(productId, request);
        return ResponseEntity.ok().body(ApiResponse.success(response));
    }

    /**
     * 상품 삭제
     */
    @Operation(summary = "상품 삭제", description = "상품을 삭제합니다. 'MASTER, HUB_MANAGER(담당 허브) 권한이 필요합니다.")
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER')")
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID productId,
            @AuthenticationPrincipal UserPrincipal userPrincipal){

        String userName = userPrincipal.username();
        productService.deleteProduct(productId, userName);
        return ResponseEntity.ok().body(ApiResponse.noContent());
    }

    /**
     * 상품 단건 조회
     */
    @Operation(summary = "상품 단건 조회", description = "업체 ID로 단일 업체 정보를 조회합니다.")
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(@PathVariable UUID productId) {
        var response = productService.getProductDetail(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    /**
     * 상품 전체 조회 (검색 페이징)
     */
    @Operation(summary = "상품 전체(검색) 조회", description = "상품 리스트를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductSearchResponse>>> getProducts(
            @RequestParam(required = false) String productName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "false") boolean isAsc
    ) {
        Page<ProductSearchResponse> productPage = productService.getProducts(productName, page, size, sortBy, isAsc)
                .map(ProductSearchResponse::from);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.fromPage(productPage)));
    }

}
