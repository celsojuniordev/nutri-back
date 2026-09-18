package com.br.nutri.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

    @Test
    void buildsFromSpringDataPageApplyingTheMapper() {
        PageImpl<Integer> page = new PageImpl<>(List.of(1, 2, 3), PageRequest.of(1, 3), 10);

        PageResponse<String> response = PageResponse.from(page, i -> "item-" + i);

        assertThat(response.content()).containsExactly("item-1", "item-2", "item-3");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(3);
        assertThat(response.totalElements()).isEqualTo(10);
        assertThat(response.totalPages()).isEqualTo(4);
    }
}
