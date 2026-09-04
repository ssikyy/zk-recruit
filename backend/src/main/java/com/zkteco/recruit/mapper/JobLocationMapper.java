package com.zkteco.recruit.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zkteco.recruit.domain.entity.JobLocation;

public interface JobLocationMapper extends BaseMapper<JobLocation> {

    @Select("SELECT * FROM job_location WHERE status = 'ENABLED' ORDER BY sort_order ASC, id ASC")
    List<JobLocation> listEnabled();

    @Select("SELECT * FROM job_location ORDER BY sort_order ASC, id ASC")
    List<JobLocation> listAll();

    @Select("SELECT COUNT(1) FROM job_location WHERE name = #{name} AND id != #{excludeId}")
    int countByNameExcluding(@Param("name") String name, @Param("excludeId") Long excludeId);

    @Select("SELECT COUNT(1) FROM job WHERE location_id = #{id}")
    int countReferences(@Param("id") Long id);
}
