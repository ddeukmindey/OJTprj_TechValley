package com.techvalley.monitor.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@Builder
@AllArgsConstructor
public class PageMeta {

    private int currentPage; // 1-based, khớp với ví dụ trong specification_OJTprj.md
    private int pageSize;
    private long totalElements;
    private int totalPages;

    public static PageMeta from(Page<?> page) {
        return PageMeta.builder()
                .currentPage(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
