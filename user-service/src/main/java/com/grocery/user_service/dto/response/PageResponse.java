package com.grocery.user_service.dto.response;

import lombok.Data;
import org.springframework.data.domain.Page;

@Data
public class PageResponse {

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public <T> PageResponse(Page<T> page) {
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
    }
}
