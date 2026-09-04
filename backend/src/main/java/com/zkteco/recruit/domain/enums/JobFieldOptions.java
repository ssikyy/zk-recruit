package com.zkteco.recruit.domain.enums;

import java.util.List;

/**
 * 学历与工作经验为固定枚举（§10.3），不做后台维护，避免与字典表混淆。
 */
public final class JobFieldOptions {

    public static final List<String> EDUCATIONS =
            List.of("不限", "高中及以上", "大专及以上", "本科及以上", "硕士及以上", "博士");

    public static final List<String> EXPERIENCES =
            List.of("不限", "1年以下", "1-3年", "3-5年", "5-10年", "10年以上");

    private JobFieldOptions() {
    }

    public static boolean validEducation(String value) {
        return value != null && EDUCATIONS.contains(value);
    }

    public static boolean validExperience(String value) {
        return value != null && EXPERIENCES.contains(value);
    }
}
