package com.zkteco.recruit.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zkteco.recruit.domain.entity.Resume;

public interface ResumeMapper extends BaseMapper<Resume> {

    @Select("SELECT * FROM resume WHERE candidate_id = #{candidateId} LIMIT 1")
    Resume findByCandidateId(@Param("candidateId") Long candidateId);
}
