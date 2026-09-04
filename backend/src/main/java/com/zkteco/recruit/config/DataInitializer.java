package com.zkteco.recruit.config;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.zkteco.recruit.common.JsonUtils;
import com.zkteco.recruit.domain.entity.CandidateProfile;
import com.zkteco.recruit.domain.entity.Job;
import com.zkteco.recruit.domain.entity.JobCategory;
import com.zkteco.recruit.domain.entity.JobLocation;
import com.zkteco.recruit.domain.entity.Resume;
import com.zkteco.recruit.domain.entity.SysUser;
import com.zkteco.recruit.domain.enums.EnableStatus;
import com.zkteco.recruit.domain.enums.JobStatus;
import com.zkteco.recruit.domain.enums.RecruitmentType;
import com.zkteco.recruit.domain.enums.TargetAudience;
import com.zkteco.recruit.domain.enums.UserRole;
import com.zkteco.recruit.mapper.CandidateProfileMapper;
import com.zkteco.recruit.mapper.JobCategoryMapper;
import com.zkteco.recruit.mapper.JobLocationMapper;
import com.zkteco.recruit.mapper.JobMapper;
import com.zkteco.recruit.mapper.ResumeMapper;
import com.zkteco.recruit.mapper.SysUserMapper;

/**
 * 初始化脚本（§16.6）：管理员 HR、字典数据，以及可选的演示数据。
 * 全部操作幂等，重复启动不会产生重复数据。
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final List<String> CATEGORIES =
            List.of("技术研发", "产品设计", "市场销售", "职能支持", "生产制造", "供应链");
    private static final List<String> LOCATIONS =
            List.of("深圳", "东莞", "北京", "上海", "广州", "成都", "西安", "武汉");

    private final AppProperties appProperties;
    private final SysUserMapper userMapper;
    private final CandidateProfileMapper profileMapper;
    private final ResumeMapper resumeMapper;
    private final JobCategoryMapper categoryMapper;
    private final JobLocationMapper locationMapper;
    private final JobMapper jobMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AppProperties appProperties,
                           SysUserMapper userMapper,
                           CandidateProfileMapper profileMapper,
                           ResumeMapper resumeMapper,
                           JobCategoryMapper categoryMapper,
                           JobLocationMapper locationMapper,
                           JobMapper jobMapper,
                           PasswordEncoder passwordEncoder) {
        this.appProperties = appProperties;
        this.userMapper = userMapper;
        this.profileMapper = profileMapper;
        this.resumeMapper = resumeMapper;
        this.categoryMapper = categoryMapper;
        this.locationMapper = locationMapper;
        this.jobMapper = jobMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!appProperties.getInit().isEnabled()) {
            return;
        }
        SysUser admin = initAdmin();
        initDictionaries();
        if (appProperties.getInit().isDemoData()) {
            initDemoHrUsers();
            initDemoJobs(admin);
            initDemoCandidates();
        }
    }

    /** 第一个 HR 账号即管理员 HR（§5.2） */
    private SysUser initAdmin() {
        String email = appProperties.getInit().getAdminEmail().toLowerCase();
        SysUser existing = userMapper.findByEmail(email);
        if (existing != null) {
            return existing;
        }
        SysUser admin = new SysUser();
        admin.setEmail(email);
        admin.setName(appProperties.getInit().getAdminName());
        admin.setPasswordHash(passwordEncoder.encode(appProperties.getInit().getAdminPassword()));
        admin.setRole(UserRole.HR);
        admin.setHrAdmin(true);
        admin.setStatus(EnableStatus.ENABLED);
        userMapper.insert(admin);
        log.info("已创建管理员 HR 账号: {}", email);
        return admin;
    }

    private void initDictionaries() {
        if (categoryMapper.listAll().isEmpty()) {
            int sort = 10;
            for (String name : CATEGORIES) {
                JobCategory category = new JobCategory();
                category.setName(name);
                category.setSortOrder(sort);
                category.setStatus(EnableStatus.ENABLED);
                categoryMapper.insert(category);
                sort += 10;
            }
            log.info("已初始化职位类别 {} 条", CATEGORIES.size());
        }
        if (locationMapper.listAll().isEmpty()) {
            int sort = 10;
            for (String name : LOCATIONS) {
                JobLocation location = new JobLocation();
                location.setName(name);
                location.setSortOrder(sort);
                location.setStatus(EnableStatus.ENABLED);
                locationMapper.insert(location);
                sort += 10;
            }
            log.info("已初始化工作地点 {} 条", LOCATIONS.size());
        }
    }

    /* ---------------- 演示数据 ---------------- */

    /** 演示环境固定补齐 4 个普通 HR，加上管理员共 5 个 HR 账号。 */
    private void initDemoHrUsers() {
        createDemoHr("hr01@ttaiagent.cn", "招聘HR一");
        createDemoHr("hr02@ttaiagent.cn", "招聘HR二");
        createDemoHr("hr03@ttaiagent.cn", "招聘HR三");
        createDemoHr("hr04@ttaiagent.cn", "招聘HR四");
    }

    private void createDemoHr(String email, String name) {
        if (userMapper.findByEmail(email) != null) {
            return;
        }
        SysUser user = new SysUser();
        user.setEmail(email);
        user.setName(name);
        user.setPasswordHash(passwordEncoder.encode("admin000"));
        user.setRole(UserRole.HR);
        user.setHrAdmin(false);
        user.setStatus(EnableStatus.ENABLED);
        userMapper.insert(user);
        log.info("已创建演示 HR 账号: {}", email);
    }

    private void initDemoJobs(SysUser owner) {
        if (jobMapper.selectCount(null) > 0) {
            return;
        }
        Map<String, Long> categories = new LinkedHashMap<>();
        for (JobCategory category : categoryMapper.listAll()) {
            categories.put(category.getName(), category.getId());
        }
        Map<String, Long> locations = new LinkedHashMap<>();
        for (JobLocation location : locationMapper.listAll()) {
            locations.put(location.getName(), location.getId());
        }

        createJob(owner, "高级 Java 后端工程师", RecruitmentType.SOCIAL,
                categories.get("技术研发"), locations.get("深圳"), 3, "本科及以上", "3-5年", null, null,
                "1. 负责智慧空间平台后端服务的设计与开发；\n2. 参与核心链路的性能优化与稳定性建设；\n3. 与算法、前端团队协作完成端到端交付。",
                "1. 计算机相关专业本科及以上，3 年以上 Java 开发经验；\n2. 熟悉 Spring Boot、MySQL、Redis，理解并发与事务；\n3. 有中大型系统重构或高并发经验者优先。");

        createJob(owner, "生物识别算法工程师", RecruitmentType.SOCIAL,
                categories.get("技术研发"), locations.get("东莞"), 2, "硕士及以上", "3-5年", null, null,
                "1. 负责人脸、指纹等多模态识别算法研发；\n2. 推进算法在嵌入式设备上的落地与优化。",
                "1. 模式识别、计算机视觉相关方向硕士及以上；\n2. 熟悉 PyTorch，有算法工程化落地经验；\n3. 有边缘设备部署经验者优先。");

        createJob(owner, "产品经理（智慧办公）", RecruitmentType.SOCIAL,
                categories.get("产品设计"), locations.get("深圳"), 1, "本科及以上", "5-10年", null, null,
                "1. 负责智慧办公产品线的规划与迭代；\n2. 输出需求文档并推动跨部门落地。",
                "1. 5 年以上 B 端产品经验；\n2. 熟悉考勤、门禁、会议等办公场景；\n3. 具备较强的方案表达与推动能力。");

        createJob(owner, "海外区域销售经理", RecruitmentType.SOCIAL,
                categories.get("市场销售"), locations.get("上海"), 2, "本科及以上", "3-5年", null, null,
                "1. 负责所辖区域渠道拓展与客户经营；\n2. 完成区域销售目标并维护重点客户关系。",
                "1. 3 年以上海外市场销售经验；\n2. 英语可作为工作语言；\n3. 有安防或物联网行业背景优先。");

        createJob(owner, "2027届 软件开发工程师（校招）", RecruitmentType.CAMPUS,
                categories.get("技术研发"), locations.get("深圳"), 10, "本科及以上", null, "2027届",
                TargetAudience.GRADUATE,
                "1. 参与公司核心产品的功能开发与测试；\n2. 在导师指导下完成模块设计与编码。",
                "1. 2027 届本科及以上应届毕业生；\n2. 计算机相关专业，掌握至少一门主流开发语言；\n3. 学习能力强，具备良好的团队协作意识。");

        createJob(owner, "2027届 算法研究实习生", RecruitmentType.CAMPUS,
                categories.get("技术研发"), locations.get("东莞"), 5, "硕士及以上", null, "2027届",
                TargetAudience.INTERN,
                "1. 参与生物识别方向的算法调研与实验；\n2. 协助完成数据处理与模型评估。",
                "1. 硕士在读，每周可实习 4 天以上；\n2. 熟悉深度学习基础理论与常用框架；\n3. 有论文或竞赛经历者优先。");

        createJob(owner, "职能支持专员（草稿示例）", RecruitmentType.SOCIAL,
                categories.get("职能支持"), locations.get("深圳"), 1, "大专及以上", "1-3年", null, null,
                "1. 负责行政与内部支持事务；\n2. 协助组织员工活动。",
                "1. 1 年以上行政或人力支持经验；\n2. 沟通表达清晰，工作细致。");

        log.info("已初始化演示职位 7 条（其中 1 条保持草稿状态）");
    }

    private void createJob(SysUser owner, String title, RecruitmentType type, Long categoryId, Long locationId,
                           int headcount, String education, String experience, String graduationYear,
                           TargetAudience audience, String duty, String requirement) {
        if (categoryId == null || locationId == null) {
            return;
        }
        Job job = new Job();
        job.setTitle(title);
        job.setRecruitmentType(type);
        job.setCategoryId(categoryId);
        job.setLocationId(locationId);
        job.setOwnerHrId(owner.getId());
        job.setCreatedBy(owner.getId());
        job.setHeadcount(headcount);
        job.setEducation(education);
        job.setExperience(experience);
        job.setGraduationYear(graduationYear);
        job.setTargetAudience(audience);
        job.setDuty(duty);
        job.setRequirement(requirement);
        job.setVersion(0);
        // 最后一条保留草稿，用于演示"草稿不在官网展示"
        boolean draft = title.contains("草稿");
        job.setStatus(draft ? JobStatus.DRAFT : JobStatus.PUBLISHED);
        job.setPublishedAt(draft ? null : LocalDateTime.now());
        jobMapper.insert(job);
    }

    /** 演示环境固定创建 5 个求职者，第一个账号带完整在线简历。 */
    private void initDemoCandidates() {
        createDemoCandidate("demo.candidate@example.com", "张演示", "13800000001", "MALE", "深圳", true);
        createDemoCandidate("candidate02@ttaiagent.cn", "李测试", "13800000002", "FEMALE", "广州", false);
        createDemoCandidate("candidate03@ttaiagent.cn", "王测试", "13800000003", "MALE", "东莞", false);
        createDemoCandidate("candidate04@ttaiagent.cn", "赵测试", "13800000004", "FEMALE", "上海", false);
        createDemoCandidate("candidate05@ttaiagent.cn", "陈测试", "13800000005", "UNKNOWN", "北京", false);
    }

    private void createDemoCandidate(String email, String name, String phone,
                                     String gender, String city, boolean withResume) {
        if (userMapper.findByEmail(email) != null) {
            return;
        }
        SysUser user = new SysUser();
        user.setEmail(email);
        user.setName(name);
        user.setPasswordHash(passwordEncoder.encode("user000"));
        user.setRole(UserRole.CANDIDATE);
        user.setHrAdmin(false);
        user.setStatus(EnableStatus.ENABLED);
        userMapper.insert(user);

        CandidateProfile profile = new CandidateProfile();
        profile.setUserId(user.getId());
        profile.setName(name);
        profile.setPhone(phone);
        profile.setGender(gender);
        profile.setCity(city);
        profileMapper.insert(profile);

        if (!withResume) {
            log.info("已创建演示求职者账号: {}", email);
            return;
        }

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("intention", Map.of(
                "expectCategory", "技术研发",
                "expectCity", "深圳",
                "expectSalary", "面议",
                "remark", ""
        ));
        content.put("educations", List.of(Map.of(
                "school", "华南理工大学",
                "major", "计算机科学与技术",
                "degree", "本科",
                "startDate", "2019-09",
                "endDate", "2023-06"
        )));
        content.put("experiences", List.of(Map.of(
                "company", "某科技有限公司",
                "position", "Java 开发工程师",
                "startDate", "2023-07",
                "endDate", "2026-08",
                "description", "负责业务中台开发与接口治理。"
        )));
        content.put("projects", List.of());
        content.put("skills", "Java、Spring Boot、MySQL、Redis");
        content.put("certificates", "");
        content.put("selfEvaluation", "习惯把问题拆开再解决，愿意为结果负责。");

        Resume resume = new Resume();
        resume.setCandidateId(user.getId());
        resume.setContent(JsonUtils.toJson(content));
        resumeMapper.insert(resume);

        log.info("已创建带在线简历的演示求职者账号: {}", email);
    }
}
