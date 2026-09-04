package com.zkteco.recruit.common;

/**
 * 分页请求参数（§15.8）：page 从 1 开始，size 默认 20 最大 100。
 */
public class PageQuery {

    private static final long DEFAULT_SIZE = 20;
    private static final long MAX_SIZE = 100;

    private Long page;
    private Long size;
    private String sort;

    public long normalizedPage() {
        return page == null || page < 1 ? 1 : page;
    }

    public long normalizedSize() {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /**
     * 解析 sort=field,asc|desc；仅接受白名单字段，防止 SQL 注入。
     */
    public String orderByOrDefault(String defaultColumn, String... allowedColumns) {
        if (sort == null || sort.isBlank()) {
            return defaultColumn;
        }
        String[] parts = sort.split(",");
        String field = toSnake(parts[0].trim());
        boolean matched = false;
        for (String allowed : allowedColumns) {
            if (allowed.equals(field)) {
                matched = true;
                break;
            }
        }
        if (!matched) {
            return defaultColumn;
        }
        boolean desc = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim());
        return field + (desc ? " DESC" : " ASC");
    }

    private String toSnake(String camel) {
        StringBuilder sb = new StringBuilder();
        for (char c : camel.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else if (Character.isLetterOrDigit(c) || c == '_') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
