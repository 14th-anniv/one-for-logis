package com.oneforlogis.product.infrastructure.persistence;

import com.oneforlogis.product.domain.model.Product;
import com.oneforlogis.product.domain.repository.ProductRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public Optional<Product> findByIdAndDeletedFalse(UUID id) {
        return productJpaRepository.findByIdAndDeletedFalse(id);
    }

    @Override
    public Page<Product> findByDeletedFalse(Pageable pageable) {
        return productJpaRepository.findByDeletedFalse(pageable);
    }

    @Override
    public Page<Product> findByNameContainingAndDeletedFalse(String keyword, Pageable pageable) {
        return productJpaRepository.findByNameContainingAndDeletedFalse(keyword, pageable);
    }
}
