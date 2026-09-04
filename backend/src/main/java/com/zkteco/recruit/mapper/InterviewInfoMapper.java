package com.zkteco.recruit.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zkteco.recruit.domain.entity.InterviewInfo;

public interface InterviewInfoMapper extends BaseMapper<InterviewInfo> {

    @Select("SELECT * FROM interview_info WHERE application_id = #{applicationId} LIMIT 1")
    InterviewInfo findByApplicationId(@Param("applicationId") Long applicationId);
}
