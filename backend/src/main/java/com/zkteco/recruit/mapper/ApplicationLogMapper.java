package com.zkteco.recruit.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zkteco.recruit.domain.entity.ApplicationLog;

public interface ApplicationLogMapper extends BaseMapper<ApplicationLog> {

    @Select("""
            SELECT l.*, u.name AS operatorName
              FROM application_log l
              LEFT JOIN sys_user u ON u.id = l.operator_id
             WHERE l.application_id = #{applicationId}
             ORDER BY l.id ASC
            """)
    List<LogRow> listByApplication(@Param("applicationId") Long applicationId);

    /**
     * 带操作人姓名的日志行。
     */
    class LogRow extends ApplicationLog {
        private String operatorName;

        public String getOperatorName() {
            return operatorName;
        }

        public void setOperatorName(String operatorName) {
            this.operatorName = operatorName;
        }
    }
}
