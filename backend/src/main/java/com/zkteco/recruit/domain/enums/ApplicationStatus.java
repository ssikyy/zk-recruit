package com.zkteco.recruit.domain.enums;

import java.util.Map;
import java.util.Set;

/**
 * 投递状态机（§12.2）。
 * <p>
 * SUBMITTED → VIEWED | WITHDRAWN<br>
 * VIEWED → INTERVIEW | REJECTED | WITHDRAWN<br>
 * INTERVIEW → PASSED | REJECTED | WITHDRAWN<br>
 * PASSED / REJECTED → VIEWED（仅 HR 撤销误操作）<br>
 * WITHDRAWN 为终态，不可变更；且只能由候选人本人发起。
 */
public enum ApplicationStatus {

    SUBMITTED("已投递", "已投递"),
    VIEWED("已查看", "处理中"),
    INTERVIEW("待面试", "待面试"),
    PASSED("已通过", "已通过"),
    REJECTED("不合适", "暂不匹配"),
    WITHDRAWN("已撤回", "已撤回");

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED = Map.of(
            SUBMITTED, Set.of(VIEWED, WITHDRAWN),
            VIEWED, Set.of(INTERVIEW, REJECTED, WITHDRAWN),
            INTERVIEW, Set.of(PASSED, REJECTED, WITHDRAWN),
            PASSED, Set.of(VIEWED),
            REJECTED, Set.of(VIEWED),
            WITHDRAWN, Set.of()
    );

    /** 允许候选人撤回的状态（§9.6） */
    private static final Set<ApplicationStatus> WITHDRAWABLE = Set.of(SUBMITTED, VIEWED, INTERVIEW);

    private final String hrLabel;
    private final String candidateLabel;

    ApplicationStatus(String hrLabel, String candidateLabel) {
        this.hrLabel = hrLabel;
        this.candidateLabel = candidateLabel;
    }

    /** HR 端显示文案 */
    public String getHrLabel() {
        return hrLabel;
    }

    /** 求职者端对外状态文案 */
    public String getCandidateLabel() {
        return candidateLabel;
    }

    public boolean canTransitTo(ApplicationStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean withdrawable() {
        return WITHDRAWABLE.contains(this);
    }

    public boolean isFinal() {
        return this == WITHDRAWN;
    }
}
