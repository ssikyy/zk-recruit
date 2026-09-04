package com.zkteco.recruit.domain.enums;

public enum RecruitmentType {
    /** 社会招聘：工作经验要求必填 */
    SOCIAL("社会招聘"),
    /** 校园招聘：毕业年份与招聘对象必填 */
    CAMPUS("校园招聘");

    private final String label;

    RecruitmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
