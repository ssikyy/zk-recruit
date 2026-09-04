package com.zkteco.recruit.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.zkteco.recruit.domain.entity.JobApplication;
import com.zkteco.recruit.domain.vo.ApplicationRowVO;

public interface JobApplicationMapper extends BaseMapper<JobApplication> {

    String ROW_SELECT = """
            SELECT a.id, a.candidate_id, a.job_id, a.attempt_no, a.status, a.applied_at,
                   a.withdrawn_at, a.last_handled_at, a.job_snapshot,
                   j.title AS jobTitle, j.recruitment_type, j.owner_hr_id,
                   l.name AS locationName, u.name AS ownerName,
                   cp.name AS candidateName, cp.phone AS candidatePhone,
                   iv.interview_time, iv.result AS interviewResult
              FROM job_application a
              JOIN job j ON j.id = a.job_id
              LEFT JOIN job_location l ON l.id = j.location_id
              LEFT JOIN sys_user u ON u.id = j.owner_hr_id
              LEFT JOIN candidate_profile cp ON cp.user_id = a.candidate_id
              LEFT JOIN interview_info iv ON iv.application_id = a.id
            """;

    @Select("<script>" + ROW_SELECT + " ${ew.customSqlSegment}</script>")
    IPage<ApplicationRowVO> selectRows(IPage<ApplicationRowVO> page,
                                       @Param(Constants.WRAPPER) Wrapper<JobApplication> wrapper);

    @Select(ROW_SELECT + " WHERE a.id = #{id}")
    ApplicationRowVO selectRow(@Param("id") Long id);

    /** 同一候选人对同一职位的全部投递（含撤回），用于次数校验与 HR 历史展示（§9.6、§10.5） */
    @Select(ROW_SELECT + " WHERE a.candidate_id = #{candidateId} AND a.job_id = #{jobId} ORDER BY a.attempt_no ASC")
    List<ApplicationRowVO> selectHistory(@Param("candidateId") Long candidateId, @Param("jobId") Long jobId);

    @Select("SELECT COUNT(1) FROM job_application WHERE candidate_id = #{candidateId} AND job_id = #{jobId}")
    int countAttempts(@Param("candidateId") Long candidateId, @Param("jobId") Long jobId);

    @Select("SELECT COUNT(1) FROM job_application WHERE candidate_id = #{candidateId} AND job_id = #{jobId} AND status != 'WITHDRAWN'")
    int countActive(@Param("candidateId") Long candidateId, @Param("jobId") Long jobId);

    /**
     * 状态变更：乐观锁 + 原状态双重条件，返回 0 表示版本冲突（§12.2 → 错误码 3004）。
     */
    @Update("""
            UPDATE job_application
               SET status = #{to}, version = version + 1, last_handled_at = NOW()
             WHERE id = #{id} AND version = #{version} AND status = #{from}
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("version") Integer version,
                     @Param("from") String from,
                     @Param("to") String to);

    /**
     * 候选人撤回，额外写入 withdrawn_at（§9.6）。
     */
    @Update("""
            UPDATE job_application
               SET status = 'WITHDRAWN', withdrawn_at = NOW(), version = version + 1
             WHERE id = #{id} AND version = #{version} AND status = #{from}
            """)
    int withdraw(@Param("id") Long id, @Param("version") Integer version, @Param("from") String from);

    @Update("UPDATE job_application SET hr_note = #{note}, version = version + 1 WHERE id = #{id} AND version = #{version}")
    int updateNote(@Param("id") Long id, @Param("version") Integer version, @Param("note") String note);

    /* ---------------- 工作台指标，口径见 §10.2 ---------------- */

    @Select("""
            <script>
            SELECT COUNT(1) FROM job_application a JOIN job j ON j.id = a.job_id
             WHERE a.status = #{status}
            <if test="ownerHrId != null"> AND j.owner_hr_id = #{ownerHrId} </if>
            </script>
            """)
    int countByStatus(@Param("status") String status, @Param("ownerHrId") Long ownerHrId);

    @Select("""
            <script>
            SELECT COUNT(1) FROM job_application a
              JOIN job j ON j.id = a.job_id
              LEFT JOIN interview_info iv ON iv.application_id = a.id
             WHERE a.status = 'INTERVIEW' AND iv.result IS NULL
            <if test="ownerHrId != null"> AND j.owner_hr_id = #{ownerHrId} </if>
            </script>
            """)
    int countPendingInterview(@Param("ownerHrId") Long ownerHrId);

    /** 负责人已被停用的职位数，仅管理员工作台展示（§10.2） */
    @Select("SELECT COUNT(1) FROM job j JOIN sys_user u ON u.id = j.owner_hr_id WHERE u.status = 'DISABLED'")
    int countJobsWithDisabledOwner();
}
