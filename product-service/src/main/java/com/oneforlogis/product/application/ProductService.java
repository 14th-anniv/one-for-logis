package com.oneforlogis.product.application;

import com.oneforlogis.product.application.dto.request.ProductCreateRequest;
import com.oneforlogis.product.application.dto.response.ProductResponse;
import com.oneforlogis.product.domain.model.Product;
import com.oneforlogis.product.domain.repository.ProductRepository;
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

}
