package com.zkteco.recruit.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.zkteco.recruit.domain.entity.Job;
import com.zkteco.recruit.domain.vo.JobRowVO;

public interface JobMapper extends BaseMapper<Job> {

    String ROW_SELECT = """
            SELECT j.id, j.title, j.recruitment_type, j.category_id, j.location_id, j.owner_hr_id,
                   j.headcount, j.education, j.experience, j.graduation_year, j.target_audience,
                   j.status, j.version, j.published_at, j.created_at,
                   c.name AS categoryName, l.name AS locationName, u.name AS ownerName,
                   (SELECT COUNT(1) FROM job_application a
                     WHERE a.job_id = j.id AND a.status != 'WITHDRAWN') AS applicationCount
              FROM job j
              LEFT JOIN job_category c ON c.id = j.category_id
              LEFT JOIN job_location l ON l.id = j.location_id
              LEFT JOIN sys_user u ON u.id = j.owner_hr_id
            """;

    /** 详情额外带出富文本，列表不取这两列以免响应体过大 */
    String DETAIL_SELECT = """
            SELECT j.id, j.title, j.recruitment_type, j.category_id, j.location_id, j.owner_hr_id,
                   j.headcount, j.education, j.experience, j.graduation_year, j.target_audience,
                   j.status, j.version, j.published_at, j.created_at, j.duty, j.requirement,
                   c.name AS categoryName, l.name AS locationName, u.name AS ownerName,
                   (SELECT COUNT(1) FROM job_application a
                     WHERE a.job_id = j.id AND a.status != 'WITHDRAWN') AS applicationCount
              FROM job j
              LEFT JOIN job_category c ON c.id = j.category_id
              LEFT JOIN job_location l ON l.id = j.location_id
              LEFT JOIN sys_user u ON u.id = j.owner_hr_id
            """;

    @Select("<script>" + ROW_SELECT + " ${ew.customSqlSegment}</script>")
    IPage<JobRowVO> selectJobRows(IPage<JobRowVO> page, @Param(Constants.WRAPPER) Wrapper<Job> wrapper);

    @Select(DETAIL_SELECT + " WHERE j.id = #{id}")
    JobRowVO selectJobRow(@Param("id") Long id);

    /**
     * 职位状态变更，带乐观锁与原状态双重条件（§12.1）。
     */
    @Update("""
            UPDATE job SET status = #{to}, version = version + 1,
                   published_at = CASE WHEN #{to} = 'PUBLISHED' AND published_at IS NULL THEN NOW() ELSE published_at END
             WHERE id = #{id} AND version = #{version} AND status = #{from}
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("version") Integer version,
                     @Param("from") String from,
                     @Param("to") String to);

    @Update("UPDATE job SET owner_hr_id = #{ownerHrId}, version = version + 1 WHERE id = #{id} AND version = #{version}")
    int updateOwner(@Param("id") Long id, @Param("version") Integer version, @Param("ownerHrId") Long ownerHrId);

    /** 含已撤回的全部投递数，用于判断能否撤回发布 / 删除（§12.1、§13.1） */
    @Select("SELECT COUNT(1) FROM job_application WHERE job_id = #{jobId}")
    int countAllApplications(@Param("jobId") Long jobId);

    @Select("SELECT COUNT(1) FROM job WHERE owner_hr_id = #{hrId}")
    int countByOwner(@Param("hrId") Long hrId);

    @Select("SELECT COUNT(1) FROM job WHERE status = 'PUBLISHED'")
    int countPublished();

    @Select("SELECT COUNT(1) FROM job WHERE status = 'PUBLISHED' AND owner_hr_id = #{hrId}")
    int countPublishedByOwner(@Param("hrId") Long hrId);
}
