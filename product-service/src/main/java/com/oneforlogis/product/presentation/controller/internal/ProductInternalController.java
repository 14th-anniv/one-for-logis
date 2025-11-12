package com.oneforlogis.product.presentation.controller.internal;

import com.oneforlogis.product.application.ProductService;
import com.oneforlogis.product.domain.model.Product;
import com.oneforlogis.product.presentation.controller.internal.dto.ProductDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/products")
@RequiredArgsConstructor
public class ProductInternalController {

    private final ProductService productService;

    // 상품 조회
    @GetMapping("/{productId}")
    public ProductDto getProductInternal(@PathVariable UUID productId){
        Product product = productService.getProductById(productId);
        return ProductDto.fromEntity(product);
    }

    // 재고 감소
    @PostMapping("/{productId}/decrease-stock")
    public ResponseEntity<ProductDto> decreaseStock(
            @PathVariable UUID productId,
            @RequestParam int amount) {

        Product updatedProduct = productService.decreaseStock(productId, amount);
        return ResponseEntity.ok(ProductDto.fromEntity(updatedProduct));
    }

    // 재고 증가
    @PostMapping("/{productId}/increase-stock")
    public ResponseEntity<ProductDto> increaseStock(
            @PathVariable UUID productId,
            @RequestParam int amount) {
        Product updatedProduct = productService.increaseStock(productId, amount);
        return ResponseEntity.ok(ProductDto.fromEntity(updatedProduct));
    }
}