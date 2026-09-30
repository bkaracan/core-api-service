package com.enterprise.coreapi.common.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Spring Data Page nesnesini ön yüz için optimize edilmiş sayfalanmış formata dönüştürür.
 */
@Getter
@Builder
public class PageResponse<T> {

    private final List<T> items;
    private final int pageNumber;
    private final int pageSize;
    private final long totalElements;
    private final int totalPages;
    private final boolean isFirst;
    private final boolean isLast;
    private final boolean hasNext;
    private final boolean hasPrevious;

    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .items(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
