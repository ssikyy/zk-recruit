package com.zkteco.recruit.domain.enums;

/**
 * 面试结果。为空表示"已约未面"，用于区分工作台的待面试口径（§10.2）。
 * 填写结果不会自动变更投递状态（D16）。
 */
public enum InterviewResult {
    PASS("通过"),
    FAIL("不合适");

    private final String label;

    InterviewResult(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
