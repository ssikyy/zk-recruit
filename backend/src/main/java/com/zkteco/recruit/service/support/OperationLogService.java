package com.zkteco.recruit.service.support;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.zkteco.recruit.common.JsonUtils;
import com.zkteco.recruit.domain.entity.SysOperationLog;
import com.zkteco.recruit.mapper.SysOperationLogMapper;

/**
 * 管理类操作审计（§16.2）：字典、HR 账号、归属转移、密码重置。
 */
@Service
public class OperationLogService {

    public static final String MODULE_DICT = "DICT";
    public static final String MODULE_HR_USER = "HR_USER";
    public static final String MODULE_JOB = "JOB";

    private final SysOperationLogMapper mapper;

    public OperationLogService(SysOperationLogMapper mapper) {
        this.mapper = mapper;
    }

    public void record(Long operatorId, String module, String action, Object targetId, Map<String, Object> detail) {
        SysOperationLog log = new SysOperationLog();
        log.setOperatorId(operatorId);
        log.setModule(module);
        log.setAction(action);
        log.setTargetId(targetId == null ? null : String.valueOf(targetId));
        log.setDetail(detail == null ? null : JsonUtils.toJson(detail));
        mapper.insert(log);
    }
}
