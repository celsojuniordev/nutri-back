package com.br.nutri.common;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class PageSizePolicyTest {

    @Test
    void acceptsEachAllowedSize() {
        for (int size : PageSizePolicy.ALLOWED_SIZES) {
            assertThatCode(() -> PageSizePolicy.validate(PageRequest.of(0, size))).doesNotThrowAnyException();
        }
    }

    @Test
    void rejectsSizeOutsideAllowedSet() {
        assertThatThrownBy(() -> PageSizePolicy.validate(PageRequest.of(0, 15)))
                .isInstanceOf(InvalidPageSizeException.class);
    }

    @Test
    void appliesDefaultSizeOfTwentyFiveWhenNoneInformed() {
        assertThatCode(() -> PageSizePolicy.validate(PageRequest.of(0, PageSizePolicy.DEFAULT_SIZE)))
                .doesNotThrowAnyException();
    }
}
