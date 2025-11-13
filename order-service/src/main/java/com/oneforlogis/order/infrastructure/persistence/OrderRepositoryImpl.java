package com.oneforlogis.order.infrastructure.persistence;

import com.oneforlogis.order.domain.model.Order;
import com.oneforlogis.order.domain.model.OrderStatus;
import com.oneforlogis.order.domain.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final JpaOrderRepository jpaRepository;
    
    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public Order save(Order order) {
        return jpaRepository.save(order);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<Order> findByFilters(OrderStatus status, UUID supplierId, UUID receiverId,
                                     LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Order> query = cb.createQuery(Order.class);
        Root<Order> root = query.from(Order.class);

        List<Predicate> predicates = new ArrayList<>();

        // deleted_at IS NULL 조건 (BaseEntity의 @SQLRestriction과 동일)
        predicates.add(cb.isNull(root.get("deletedAt")));

        // 필터 조건 추가
        if (status != null) {
            predicates.add(cb.equal(root.get("status"), status));
        }
        if (supplierId != null) {
            predicates.add(cb.equal(root.get("supplierCompanyId"), supplierId));
        }
        if (receiverId != null) {
            predicates.add(cb.equal(root.get("receiverCompanyId"), receiverId));
        }
        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
        }
        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
        }

        query.where(predicates.toArray(new Predicate[0]));

        // 정렬 적용
        List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();
        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(sortOrder -> {
                if (sortOrder.isAscending()) {
                    orders.add(cb.asc(root.get(sortOrder.getProperty())));
                } else {
                    orders.add(cb.desc(root.get(sortOrder.getProperty())));
                }
            });
        }
        if (orders.isEmpty()) {
            // 기본 정렬: createdAt DESC
            orders.add(cb.desc(root.get("createdAt")));
        }
        query.orderBy(orders);

        // 페이징을 위한 카운트 쿼리
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Order> countRoot = countQuery.from(Order.class);
        countQuery.select(cb.count(countRoot));
        
        List<Predicate> countPredicates = new ArrayList<>();
        countPredicates.add(cb.isNull(countRoot.get("deletedAt")));
        if (status != null) {
            countPredicates.add(cb.equal(countRoot.get("status"), status));
        }
        if (supplierId != null) {
            countPredicates.add(cb.equal(countRoot.get("supplierCompanyId"), supplierId));
        }
        if (receiverId != null) {
            countPredicates.add(cb.equal(countRoot.get("receiverCompanyId"), receiverId));
        }
        if (startDate != null) {
            countPredicates.add(cb.greaterThanOrEqualTo(countRoot.get("createdAt"), startDate));
        }
        if (endDate != null) {
            countPredicates.add(cb.lessThanOrEqualTo(countRoot.get("createdAt"), endDate));
        }
        countQuery.where(countPredicates.toArray(new Predicate[0]));

        // 카운트 조회
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        // 페이징 적용하여 조회
        TypedQuery<Order> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Order> results = typedQuery.getResultList();

        return new PageImpl<>(results, pageable, total);
    }
}

