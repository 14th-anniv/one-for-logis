package com.oneforlogis.product.application;

import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.product.application.dto.request.ProductCreateRequest;
import com.oneforlogis.product.application.dto.request.ProductUpdateRequest;
import com.oneforlogis.product.application.dto.response.ProductResponse;
import com.oneforlogis.product.application.dto.response.ProductUpdateResponse;
import com.oneforlogis.product.domain.model.Product;
import com.oneforlogis.product.domain.repository.ProductRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    // 상품 생성
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request){

        Product product = Product.createProduct(
                request.name(),
                request.quantity(),
                request.price(),
                request.hubId(),
                request.companyId()
        );

        Product savedProduct = productRepository.save(product);
        return ProductResponse.from(savedProduct);
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



    public Product getProductById(UUID productId){
        return productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
