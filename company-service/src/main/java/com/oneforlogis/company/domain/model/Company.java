package com.oneforlogis.company.domain.model;

import com.oneforlogis.common.model.BaseEntity;
import com.oneforlogis.company.application.dto.request.CompanyCreateRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_company")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CompanyType type;

    @Column(nullable = false)
    private UUID hubId;

    @Column(nullable = false)
    private String address;

    @Builder
    public Company(String name, CompanyType type, UUID hubId, String address) {
        this.name = name;
        this.type = type;
        this.hubId = hubId;
        this.address = address;
    }


    /**
     * 비즈니스 로직
     */

    public static Company createCompany(
            String name, CompanyType type, UUID hubId, String address){
        return Company.builder()
                .name(name)
                .type(type)
                .hubId(hubId)
                .address(address)
                .build();
    }

    /**
     * 업데이트 필드 - 업체명, 타입, 주소
     */
    public void updateName(String name) {
        this.name = name;
    }
    public void updateType(CompanyType type) {
        this.type = type;
    }
    public void updateAddress(String address) {
        this.address = address;
    }

    /**
     * soft del
     */
    public void deleteCompany(String userName) {
        this.markAsDeleted(userName);
    }
}