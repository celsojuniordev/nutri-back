package com.br.nutri.common;

import java.util.Set;
import org.springframework.data.domain.Pageable;

public final class PageSizePolicy {

    public static final Set<Integer> ALLOWED_SIZES = Set.of(10, 25, 50);

    public static final int DEFAULT_SIZE = 25;

    private PageSizePolicy() {
    }

    public static void validate(Pageable pageable) {
        if (!ALLOWED_SIZES.contains(pageable.getPageSize())) {
            throw new InvalidPageSizeException();
        }
    }
}
