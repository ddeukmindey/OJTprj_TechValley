package com.techvalley.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> items;
    private PageMeta pagination;

    public static <T> PageResponse<T> of(List<T> items, Page<?> sourcePage) {
        return PageResponse.<T>builder()
                .items(items)
                .pagination(PageMeta.from(sourcePage))
                .build();
    }
}