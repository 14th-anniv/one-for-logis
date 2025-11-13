package com.oneforlogis.delivery.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.delivery.application.dto.response.DeliveryStaffResponse;
import com.oneforlogis.delivery.domain.model.DeliveryStaff;
import com.oneforlogis.delivery.domain.model.DeliveryStaffType;
import com.oneforlogis.delivery.domain.repository.DeliveryRepository;
import com.oneforlogis.delivery.domain.repository.DeliveryStaffRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class DeliveryStaffServiceTest {

    @Mock
    DeliveryRepository deliveryRepository; // register()에서 사용, 여기선 안 씀

    @Mock
    DeliveryStaffRepository deliveryStaffRepository;

    @InjectMocks
    DeliveryStaffService deliveryStaffService;

    @Test
    @DisplayName("다음 배정 대상 조회 - 성공")
    void getNextStaff_success() {
        // given
        UUID hubId = UUID.randomUUID();

        DeliveryStaff staff = DeliveryStaff.create(
                hubId,
                DeliveryStaffType.IN_HOUSE,  // enum 값은 프로젝트 enum에 맞게 사용
                "U123456",
                1,
                true
        );

        Page<DeliveryStaff> page = new PageImpl<>(List.of(staff));
        when(deliveryStaffRepository.findNextStaff(eq(hubId), any(Pageable.class)))
                .thenReturn(page);

        // when
        DeliveryStaffResponse response = deliveryStaffService.getNextStaff(hubId);

        // then
        // lastAssignedAt 가 갱신되었는지
        assertNotNull(staff.getLastAssignedAt(), "lastAssignedAt가 갱신되어야 합니다.");

        // 응답 필드 매핑 확인
        assertEquals(staff.getHubId(), response.hubId());
        assertEquals(staff.getStaffType(), response.staffType());
        assertEquals(staff.getSlackId(), response.slackId());
        assertEquals(staff.getAssignOrder(), response.assignOrder());
        assertEquals(staff.getIsActive(), response.isActive());

        // PageRequest(0,1)으로 호출됐는지 검증
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(deliveryStaffRepository).findNextStaff(eq(hubId), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(1, pageable.getPageSize());
    }

    @Test
    @DisplayName("다음 배정 대상 조회 - 배정 가능한 직원이 없으면 NO_ACTIVE_STAFF 예외 발생")
    void getNextStaff_noActiveStaff_throwsException() {
        // given
        UUID hubId = UUID.randomUUID();
        Page<DeliveryStaff> emptyPage = Page.empty();

        when(deliveryStaffRepository.findNextStaff(eq(hubId), any(Pageable.class)))
                .thenReturn(emptyPage);

        // when
        CustomException ex = assertThrows(
                CustomException.class,
                () -> deliveryStaffService.getNextStaff(hubId)
        );

        // then
        assertEquals(ErrorCode.NO_ACTIVE_STAFF, ex.getErrorCode());
    }
}