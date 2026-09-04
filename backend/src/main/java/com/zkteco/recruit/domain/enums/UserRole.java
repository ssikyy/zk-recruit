package com.zkteco.recruit.domain.enums;

public enum UserRole {
    /** 求职者 */
    CANDIDATE,
    /** HR（是否管理员由 sys_user.hr_admin 权限位区分） */
    HR
}
