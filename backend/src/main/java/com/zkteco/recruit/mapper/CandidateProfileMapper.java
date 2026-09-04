package com.zkteco.recruit.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zkteco.recruit.domain.entity.CandidateProfile;

public interface CandidateProfileMapper extends BaseMapper<CandidateProfile> {

    @Select("SELECT * FROM candidate_profile WHERE user_id = #{userId} LIMIT 1")
    CandidateProfile findByUserId(@Param("userId") Long userId);
}
