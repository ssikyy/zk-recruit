package com.zkteco.recruit.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zkteco.recruit.domain.entity.JobCategory;

public interface JobCategoryMapper extends BaseMapper<JobCategory> {

    /** 官网筛选下拉只返回启用项（§10.7） */
    @Select("SELECT * FROM job_category WHERE status = 'ENABLED' ORDER BY sort_order ASC, id ASC")
    List<JobCategory> listEnabled();

    @Select("SELECT * FROM job_category ORDER BY sort_order ASC, id ASC")
    List<JobCategory> listAll();

    @Select("SELECT COUNT(1) FROM job_category WHERE name = #{name} AND id != #{excludeId}")
    int countByNameExcluding(@Param("name") String name, @Param("excludeId") Long excludeId);

    @Select("SELECT COUNT(1) FROM job WHERE category_id = #{id}")
    int countReferences(@Param("id") Long id);

    @Select("SELECT COUNT(1) FROM job WHERE category_id = #{id} AND status = 'PUBLISHED'")
    int countPublished(@Param("id") Long id);
}
