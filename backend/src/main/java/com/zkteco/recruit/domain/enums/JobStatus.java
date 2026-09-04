package com.zkteco.recruit.domain.enums;

import java.util.Map;
import java.util.Set;

/**
 * 职位状态机（§12.1）。
 * DRAFT → PUBLISHED；PUBLISHED → CLOSED；PUBLISHED → DRAFT（仅无投递）；CLOSED → PUBLISHED。
 * CLOSED → DRAFT 不允许。
 */
public enum JobStatus {

    DRAFT("草稿"),
    PUBLISHED("招聘中"),
    CLOSED("已关闭");

    private static final Map<JobStatus, Set<JobStatus>> ALLOWED = Map.of(
            DRAFT, Set.of(PUBLISHED),
            PUBLISHED, Set.of(CLOSED, DRAFT),
            CLOSED, Set.of(PUBLISHED)
    );

    private final String label;

    JobStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean canTransitTo(JobStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }
}
