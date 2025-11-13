package com.oneforlogis.delivery.domain.repository;

import com.oneforlogis.delivery.domain.model.DeliveryStaff;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DeliveryStaffRepository extends JpaRepository<DeliveryStaff, Long> {

    Page<DeliveryStaff> findByHubId(UUID hubId, Pageable pageable);

    @Query("""
            SELECT s FROM DeliveryStaff s
            WHERE s.hubId = :hubId
              AND s.isActive = true
            ORDER BY 
              CASE WHEN s.lastAssignedAt IS NULL THEN 0 ELSE 1 END,
              s.lastAssignedAt ASC,
              s.createdAt ASC
            """)
    Page<DeliveryStaff> findNextStaff(UUID hubId, Pageable pageable);
}