package com.zkteco.recruit.web;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zkteco.recruit.common.ApiResult;
import com.zkteco.recruit.common.PageQuery;
import com.zkteco.recruit.common.PageResult;
import com.zkteco.recruit.domain.enums.JobFieldOptions;
import com.zkteco.recruit.domain.enums.RecruitmentType;
import com.zkteco.recruit.domain.vo.JobRowVO;
import com.zkteco.recruit.dto.dict.DictItemVO;
import com.zkteco.recruit.service.DictService;
import com.zkteco.recruit.service.JobService;

/**
 * 公开接口（§15.1）：无需登录，免 CSRF 校验（无状态变更）。
 * <p>
 * 首页文案与配图由前端维护（§7.2），本层不提供首页内容接口。
 */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final JobService jobService;
    private final DictService dictService;

    public PublicController(JobService jobService, DictService dictService) {
        this.jobService = jobService;
        this.dictService = dictService;
    }

    @GetMapping("/jobs")
    public ApiResult<PageResult<JobRowVO>> jobs(@RequestParam(required = false) RecruitmentType type,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Long categoryId,
                                                @RequestParam(required = false) Long locationId,
                                                @ModelAttribute PageQuery pageQuery) {
        return ApiResult.ok(jobService.publicPage(type, keyword, categoryId, locationId, pageQuery));
    }

    @GetMapping("/jobs/{id}")
    public ApiResult<JobRowVO> jobDetail(@PathVariable Long id) {
        return ApiResult.ok(jobService.publicDetail(id));
    }

    @GetMapping("/job-categories")
    public ApiResult<List<DictItemVO>> categories() {
        return ApiResult.ok(dictService.publicCategories());
    }

    @GetMapping("/job-locations")
    public ApiResult<List<DictItemVO>> locations() {
        return ApiResult.ok(dictService.publicLocations());
    }

    /** 学历与工作经验的固定选项，供职位表单与前台筛选展示 */
    @GetMapping("/job-options")
    public ApiResult<Map<String, Object>> jobOptions() {
        return ApiResult.ok(Map.of(
                "educations", JobFieldOptions.EDUCATIONS,
                "experiences", JobFieldOptions.EXPERIENCES
        ));
    }
}
