package com.zkteco.recruit.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Storage storage = new Storage();
    private Apply apply = new Apply();
    private Security security = new Security();
    private Init init = new Init();

    @Data
    public static class Storage {
        /** 简历附件存储根目录，禁止公开访问（D7） */
        private String root = "./storage";
        private String resumeDir = "resume";
        private long resumeMaxSize = 10 * 1024 * 1024L;
    }

    @Data
    public static class Apply {
        /** 同一候选人对同一职位的投递次数上限（§9.6） */
        private int maxAttempts = 3;
    }

    @Data
    public static class Security {
        private int loginFailThreshold = 5;
        private int loginLockMinutes = 10;
        private int registerIpLimitPerHour = 10;
        private int resumeUploadLimitPerHour = 10;
        private int withdrawLimitPerHour = 20;
        private List<String> allowedOrigins = List.of("http://localhost:5173", "http://127.0.0.1:5173");
    }

    @Data
    public static class Init {
        private boolean enabled = true;
        private String adminEmail = "hr.admin@zkteco.com";
        private String adminPassword = "Admin@2026";
        private String adminName = "系统管理员";
        private boolean demoData = true;
    }
}
