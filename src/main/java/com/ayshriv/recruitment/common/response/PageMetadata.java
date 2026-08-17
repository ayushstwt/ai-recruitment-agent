package com.ayshriv.recruitment.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

/**
 * Pagination metadata attached to paginated {@link ApiResponse} responses.
 *
 * <p>Created automatically from a Spring Data {@link Page} by
 * {@link ResponseUtil#successPage(String, Page, String)}.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageMetadata {

    /**
     * Zero based page number.
     */
    private int page;

    /**
     * Page size.
     */
    private int size;

    /**
     * Total number of items across all pages.
     */
    private long totalElements;

    /**
     * Total number of pages.
     */
    private int totalPages;

    /**
     * Whether more pages exist after the current one.
     */
    private boolean hasNext;

    /**
     * Whether pages exist before the current one.
     */
    private boolean hasPrevious;

    /**
     * Build metadata from a Spring Data page.
     *
     * @param page the Spring Data page
     * @return pagination metadata
     */
    public static PageMetadata of(Page<?> page) {
        return PageMetadata.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}