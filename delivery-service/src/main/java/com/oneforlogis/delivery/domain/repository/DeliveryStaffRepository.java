package com.oneforlogis.delivery.domain.repository;

import com.oneforlogis.delivery.domain.model.DeliveryStaff;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryStaffRepository extends JpaRepository<DeliveryStaff, Long> {

    Page<DeliveryStaff> findByHubId(UUID hubId, Pageable pageable);
}