package com.oneforlogis.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@Builder
public class PageResponse<T> {

    private final List<T> content;

    private final long totalElements;
    private final int totalPages;
    private final int page;
    private final int size;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String sort;

    public static <T> PageResponse<T> fromPage(Page<T> pageData) {
        return PageResponse.<T>builder()
                .content(pageData.getContent())
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .page(pageData.getNumber())
                .size(pageData.getSize())
                .sort(pageData.getSort().toString())
                .build();
    }

    public static <T> PageResponse<T> of(List<T> content, long totalElements, int totalPages,
            int page, int size) {
        return PageResponse.<T>builder()
                .content(content == null ? Collections.emptyList() : content)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .page(page)
                .size(size)
                .build();
    }
}
