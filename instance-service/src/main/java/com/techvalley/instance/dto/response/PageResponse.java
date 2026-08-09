package com.techvalley.instance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> items;
    private PaginationInfo pagination;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationInfo {
        private int currentPage;
        private int pageSize;
        private long totalElements;
        private int totalPages;
    }

    public static <T> PageResponse<T> of(List<T> items, Page<?> page) {
        return PageResponse.<T>builder()
                .items(items)
                .pagination(PaginationInfo.builder()
                        .currentPage(page.getNumber() + 1)
                        .pageSize(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }
}
