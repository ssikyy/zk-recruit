package com.zkteco.recruit.dto.dict;

import com.zkteco.recruit.domain.enums.EnableStatus;

import lombok.Data;

@Data
public class DictItemVO {

    private Long id;
    private String name;
    private Integer sortOrder;
    private EnableStatus status;
    /** 引用该项的职位数量，>0 时不允许删除（§10.7） */
    private Integer referenceCount;
    /** 官网在招职位数（status = PUBLISHED），仅公开接口返回 */
    private Integer publishedCount;

    public static DictItemVO of(Long id, String name, Integer sortOrder, EnableStatus status, Integer referenceCount) {
        DictItemVO vo = new DictItemVO();
        vo.setId(id);
        vo.setName(name);
        vo.setSortOrder(sortOrder);
        vo.setStatus(status);
        vo.setReferenceCount(referenceCount);
        return vo;
    }
}
