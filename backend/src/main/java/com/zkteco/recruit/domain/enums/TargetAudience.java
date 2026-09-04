package com.zkteco.recruit.domain.enums;

/**
 * 校招招聘对象（§7.4、§10.3）。
 */
public enum TargetAudience {
    GRADUATE("应届生"),
    INTERN("实习生");

    private final String label;

    TargetAudience(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
