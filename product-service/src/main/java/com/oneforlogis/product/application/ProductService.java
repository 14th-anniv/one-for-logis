package com.oneforlogis.product.application;

import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.product.application.dto.request.ProductCreateRequest;
import com.oneforlogis.product.application.dto.request.ProductUpdateRequest;
import com.oneforlogis.product.application.dto.response.ProductCreateResponse;
import com.oneforlogis.product.application.dto.response.ProductDetailResponse;
import com.oneforlogis.product.application.dto.response.ProductUpdateResponse;
import com.oneforlogis.product.domain.model.Product;
import com.oneforlogis.product.domain.repository.ProductRepository;
import com.oneforlogis.product.infrastructure.client.CompanyClient;
import com.oneforlogis.product.infrastructure.client.HubClient;
import feign.FeignException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final CompanyClient companyClient;
    private final HubClient hubClient;
    private final ProductRepository productRepository;

    // 상품 생성
    @Transactional
    public ProductCreateResponse createProduct(ProductCreateRequest request){

        fetchCompany(request.companyId());
        fetchHub(request.hubId());
        Product product = Product.createProduct(
                request.name(),
                request.quantity(),
                request.price(),
                request.hubId(),
                request.companyId()
        );

        Product savedProduct = productRepository.save(product);
        return ProductCreateResponse.from(savedProduct);
    }

    // 상품 수정
    @Transactional
    public ProductUpdateResponse updateProduct(UUID productId, ProductUpdateRequest request){
        Product product = getProductById(productId);

        if (request.name() != null) {
            product.updateName(request.name());
        }
        if (request.quantity() != null) {
            product.updateQuantity(request.quantity());
        }
        if (request.price() != null) {
            product.updatePrice(request.price());
        }
        return ProductUpdateResponse.from(product);
    }

    // 상품 삭제
    @Transactional
    public void deleteProduct(UUID productId, String userName){
        Product product = getProductById(productId);
        product.deleteProduct(userName);
    }

    // 상품 단건 조회
    public ProductDetailResponse getProductDetail(UUID productId){
        Product product = getProductById(productId);
        return ProductDetailResponse.from(product);
    }

    // 상품 조회 (전체 + 이름 search)
    public Page<Product> getProducts(String productName, int page, int size, String sortBy, boolean isAsc) {
        Pageable pageable = createPageable(page, size, sortBy, isAsc);

        if (productName == null || productName.isBlank()) {
            return productRepository.findByDeletedFalse(pageable);
        }
        return productRepository.findByNameContainingAndDeletedFalse(productName, pageable);
    }



    /**
     * 내부용 메서드 <-> internal
     * @param productId 상품 ID
     * @param amount 수량
     */
    @Transactional
    public Product decreaseStock(UUID productId, int amount) { // 1. void -> Product
        if (amount <= 0) {
            throw new CustomException(ErrorCode.STOCK_NOT_ENOUGH);
        }
        Product product = getProductById(productId);
        product.decreaseStock(amount);

        return product;
    }

    @Transactional
    public Product increaseStock(UUID productId, int amount) { // 1. void -> Product
        if (amount <= 0) {
            throw new CustomException(ErrorCode.INVALID_RESTOCK_AMOUNT);
        }
        Product product = getProductById(productId);
        product.increaseStock(amount);

        return product; // 2. 변경된 product 반환
    }



    /**
     * 헬퍼
     */
    public Product getProductById(UUID productId){
        return productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private Pageable createPageable(int page, int size, String sortBy, boolean isAsc) {
        int validatedSize = List.of(10, 30, 50).contains(size) ? size : 10;
        int validatedPage = Math.max(page, 0); // 무조건 0부터 유효 (음수 방지 코드)
        Sort.Direction direction = isAsc ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(validatedPage, validatedSize, Sort.by(direction, sortBy));
    }


    /**
     * feign 통신
     */

    public void fetchHub(UUID hubId) {
        try {
            hubClient.getHub(hubId);
        } catch (FeignException.NotFound e) {
            throw new CustomException(ErrorCode.HUB_NOT_FOUND);
        }
    }

    public void fetchCompany(UUID companyId) {
        try {
            companyClient.getCompany(companyId);
        } catch (FeignException.NotFound e) {
            throw new CustomException(ErrorCode.COMPANY_NOT_FOUND);
        }
    }

}
