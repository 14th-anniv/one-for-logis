package com.oneforlogis.product.domain.repository;

import com.oneforlogis.product.domain.model.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository {

    Product save(Product product);
    Optional<Product> findByIdAndDeletedFalse(UUID id);
    Page<Product> findByDeletedFalse(Pageable pageable);
    Page<Product> findByNameContainingAndDeletedFalse(String keyword, Pageable pageable);
}
