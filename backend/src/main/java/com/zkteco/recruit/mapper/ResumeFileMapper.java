package com.zkteco.recruit.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zkteco.recruit.domain.entity.ResumeFile;

public interface ResumeFileMapper extends BaseMapper<ResumeFile> {

    @Select("SELECT * FROM resume_file WHERE candidate_id = #{candidateId} AND is_current = 1 LIMIT 1")
    ResumeFile findCurrent(@Param("candidateId") Long candidateId);

    /** 上传新附件或取消当前附件时，把该候选人此前的当前标记全部置 0；文件记录本身不删除（§9.3） */
    @Update("UPDATE resume_file SET is_current = 0 WHERE candidate_id = #{candidateId} AND is_current = 1")
    int clearCurrent(@Param("candidateId") Long candidateId);

    /** 是否被任意投递快照引用，被引用的文件不可删除 */
    @Select("SELECT COUNT(1) FROM job_application WHERE resume_file_id = #{fileId}")
    int countReferences(@Param("fileId") Long fileId);
}
