package com.example.unbox_common.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public class PageSizeLimiter {

    private static final Set<Integer> ALLOWED_SIZES = Set.of(10, 30, 50);
    private static final int DEFAULT_SIZE = 10;

    private PageSizeLimiter() {}

    public static Pageable limit(Pageable pageable) {
        int requestedSize = pageable.getPageSize();
        int size = ALLOWED_SIZES.contains(requestedSize) ? requestedSize : DEFAULT_SIZE;

        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.unsorted();
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }
}
