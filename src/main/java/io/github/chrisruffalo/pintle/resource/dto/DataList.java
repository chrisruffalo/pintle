package io.github.chrisruffalo.pintle.resource.dto;

import java.util.LinkedList;
import java.util.List;

public class DataList<ITEM_TYPE> {

    private List<ITEM_TYPE> data = new LinkedList<>();

    private long pageIndex = 0;

    private long pageSize = 100;

    private long totalPages = 0;

    private long totalCount = 0;

    public List<ITEM_TYPE> getData() {
        return data;
    }

    public void setData(List<ITEM_TYPE> data) {
        this.data = data;
    }

    public long getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(long pageIndex) {
        this.pageIndex = pageIndex;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(long totalPages) {
        this.totalPages = totalPages;
    }
}
