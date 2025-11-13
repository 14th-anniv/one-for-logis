package com.oneforlogis.product.presentation.controller.internal;

import com.oneforlogis.product.application.ProductService;
import com.oneforlogis.product.domain.model.Product;
import com.oneforlogis.product.presentation.controller.internal.dto.ProductDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Internal products", description = "내부용 상품 API")
@RestController
@RequestMapping("/api/v1/internal/products")
@RequiredArgsConstructor
public class ProductInternalController {

    private final ProductService productService;

    // 상품 조회
    @Operation(summary = "상품 id로 단일 조회", description = "상품 ID로 단일 상품 정보를 조회합니다.")
    @GetMapping("/{productId}")
    public ProductDto getProductInternal(@PathVariable UUID productId){
        Product product = productService.getProductById(productId);
        return ProductDto.fromEntity(product);
    }

    // 재고 감소
    @Operation(summary = "상품 재고 차감", description = "주어진 상품 ID에 대해 재고를 지정한 수량만큼 차감합니다.")
    @PostMapping("/{productId}/decrease-stock")
    public ResponseEntity<ProductDto> decreaseStock(
            @PathVariable UUID productId,
            @RequestParam int amount) {

        Product updatedProduct = productService.decreaseStock(productId, amount);
        return ResponseEntity.ok(ProductDto.fromEntity(updatedProduct));
    }

    // 재고 증가
    @Operation(summary = "상품 재고 증가", description = "주어진 상품 ID에 대해 재고를 지정한 수량만큼 증가시킵니다. ")
    @PostMapping("/{productId}/increase-stock")
    public ResponseEntity<ProductDto> increaseStock(
            @PathVariable UUID productId,
            @RequestParam int amount) {
        Product updatedProduct = productService.increaseStock(productId, amount);
        return ResponseEntity.ok(ProductDto.fromEntity(updatedProduct));
    }
}