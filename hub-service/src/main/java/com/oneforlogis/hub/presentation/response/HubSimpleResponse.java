package com.oneforlogis.hub.presentation.response;

import com.oneforlogis.hub.domain.model.Hub;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "허브 간소화 응답 DTO")
public record HubSimpleResponse(
        @Schema(description = "허브 ID (UUID)", example = "d0c14c9e-08f7-46c2-a4a6-c79abfa58f56")
        UUID id,

        @Schema(description = "허브명", example = "서울 센터")
        String name,

        @Schema(description = "허브 주소", example = "서울특별시 송파구 송파대로 55")
        String address
) {

    public HubSimpleResponse from(Hub hub) {
        return new HubSimpleResponse(
                hub.getId(),
                hub.getName(),
                hub.getAddress()
        );
    }

    public static HubSimpleResponse of(HubResponse hub) {
        return new HubSimpleResponse(
                hub.id(),
                hub.name(),
                hub.address()
        );
    }
}
