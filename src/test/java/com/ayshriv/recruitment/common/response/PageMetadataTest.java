package com.ayshriv.recruitment.common.response;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageMetadataTest {

    @Test
    void ofConvertsFirstPage() {
        Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 20), 125);

        PageMetadata metadata = PageMetadata.of(page);

        assertThat(metadata.getPage()).isZero();
        assertThat(metadata.getSize()).isEqualTo(20);
        assertThat(metadata.getTotalElements()).isEqualTo(125);
        assertThat(metadata.getTotalPages()).isEqualTo(7);
        assertThat(metadata.isHasNext()).isTrue();
        assertThat(metadata.isHasPrevious()).isFalse();
    }

    @Test
    void ofConvertsLastPage() {
        Page<String> page = new PageImpl<>(List.of("a"), PageRequest.of(6, 20), 125);

        PageMetadata metadata = PageMetadata.of(page);

        assertThat(metadata.getPage()).isEqualTo(6);
        assertThat(metadata.getTotalPages()).isEqualTo(7);
        assertThat(metadata.isHasNext()).isFalse();
        assertThat(metadata.isHasPrevious()).isTrue();
    }

    @Test
    void builderPopulatesEveryField() {
        PageMetadata metadata = PageMetadata.builder()
                .page(1)
                .size(10)
                .totalElements(50)
                .totalPages(5)
                .hasNext(true)
                .hasPrevious(true)
                .build();

        assertThat(metadata.getPage()).isEqualTo(1);
        assertThat(metadata.getSize()).isEqualTo(10);
        assertThat(metadata.getTotalElements()).isEqualTo(50);
        assertThat(metadata.getTotalPages()).isEqualTo(5);
        assertThat(metadata.isHasNext()).isTrue();
        assertThat(metadata.isHasPrevious()).isTrue();
    }
}