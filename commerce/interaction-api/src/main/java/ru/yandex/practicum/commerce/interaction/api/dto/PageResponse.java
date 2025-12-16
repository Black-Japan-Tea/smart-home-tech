package ru.yandex.practicum.commerce.interaction.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int size;
    private int number;
    private List<SortOrder> sort;
    private boolean first;
    private boolean last;
    private int numberOfElements;
    private PageableMetadata pageable;
    private boolean empty;

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .size(page.getSize())
                .number(page.getNumber())
                .sort(SortOrder.from(page.getSort()))
                .first(page.isFirst())
                .last(page.isLast())
                .numberOfElements(page.getNumberOfElements())
                .pageable(PageableMetadata.from(page.getPageable()))
                .empty(page.isEmpty())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SortOrder {
        private String property;
        private String direction;

        public static List<SortOrder> from(Sort sort) {
            if (sort == null || sort.isEmpty()) {
                return List.of();
            }
            return sort.stream()
                    .map(order -> SortOrder.builder()
                            .property(order.getProperty())
                            .direction(order.getDirection().name())
                            .build())
                    .collect(Collectors.toList());
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageableMetadata {
        private int pageNumber;
        private int pageSize;
        private long offset;
        private boolean paged;
        private boolean unpaged;
        private List<SortOrder> sort;

        public static PageableMetadata from(Pageable pageable) {
            return PageableMetadata.builder()
                    .pageNumber(pageable.getPageNumber())
                    .pageSize(pageable.getPageSize())
                    .offset(pageable.getOffset())
                    .paged(pageable.isPaged())
                    .unpaged(pageable.isUnpaged())
                    .sort(SortOrder.from(pageable.getSort()))
                    .build();
        }
    }
}

