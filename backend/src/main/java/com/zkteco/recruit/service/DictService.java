package com.zkteco.recruit.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zkteco.recruit.common.BizException;
import com.zkteco.recruit.common.ErrorCode;
import com.zkteco.recruit.domain.entity.JobCategory;
import com.zkteco.recruit.domain.entity.JobLocation;
import com.zkteco.recruit.domain.enums.EnableStatus;
import com.zkteco.recruit.dto.dict.DictItemRequest;
import com.zkteco.recruit.dto.dict.DictItemVO;
import com.zkteco.recruit.mapper.JobCategoryMapper;
import com.zkteco.recruit.mapper.JobLocationMapper;
import com.zkteco.recruit.service.support.OperationLogService;

/**
 * 职位类别与工作地点字典（§10.7）。
 * 规则：名称唯一；只停用不物理删除；停用项不出现在官网筛选与职位新建下拉中，
 * 但历史职位仍可正常展示。
 */
@Service
public class DictService {

    private final JobCategoryMapper categoryMapper;
    private final JobLocationMapper locationMapper;
    private final OperationLogService operationLogService;

    public DictService(JobCategoryMapper categoryMapper,
                       JobLocationMapper locationMapper,
                       OperationLogService operationLogService) {
        this.categoryMapper = categoryMapper;
        this.locationMapper = locationMapper;
        this.operationLogService = operationLogService;
    }

    /* ---------------- 官网公开查询：只返回启用项 ---------------- */

    public List<DictItemVO> publicCategories() {
        return categoryMapper.listEnabled().stream()
                .map(c -> {
                    DictItemVO vo = DictItemVO.of(c.getId(), c.getName(), c.getSortOrder(), c.getStatus(), null);
                    vo.setPublishedCount(categoryMapper.countPublished(c.getId()));
                    return vo;
                })
                .toList();
    }

    public List<DictItemVO> publicLocations() {
        return locationMapper.listEnabled().stream()
                .map(l -> DictItemVO.of(l.getId(), l.getName(), l.getSortOrder(), l.getStatus(), null))
                .toList();
    }

    /* ---------------- 管理端：全部项 + 引用数 ---------------- */

    public List<DictItemVO> adminCategories() {
        return categoryMapper.listAll().stream()
                .map(c -> DictItemVO.of(c.getId(), c.getName(), c.getSortOrder(), c.getStatus(),
                        categoryMapper.countReferences(c.getId())))
                .toList();
    }

    public List<DictItemVO> adminLocations() {
        return locationMapper.listAll().stream()
                .map(l -> DictItemVO.of(l.getId(), l.getName(), l.getSortOrder(), l.getStatus(),
                        locationMapper.countReferences(l.getId())))
                .toList();
    }

    /* ---------------- 职位类别写操作 ---------------- */

    @Transactional
    public Long createCategory(Long operatorId, DictItemRequest request) {
        String name = request.getName().trim();
        if (categoryMapper.countByNameExcluding(name, -1L) > 0) {
            throw BizException.of(ErrorCode.DICT_NAME_DUPLICATED);
        }
        JobCategory entity = new JobCategory();
        entity.setName(name);
        entity.setSortOrder(request.getSortOrder() == null ? nextCategorySort() : request.getSortOrder());
        entity.setStatus(EnableStatus.ENABLED);
        categoryMapper.insert(entity);
        operationLogService.record(operatorId, OperationLogService.MODULE_DICT, "CREATE_CATEGORY",
                entity.getId(), Map.of("name", name));
        return entity.getId();
    }

    @Transactional
    public void updateCategory(Long operatorId, Long id, DictItemRequest request) {
        JobCategory entity = categoryMapper.selectById(id);
        if (entity == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "职位类别不存在");
        }
        String name = request.getName().trim();
        if (categoryMapper.countByNameExcluding(name, id) > 0) {
            throw BizException.of(ErrorCode.DICT_NAME_DUPLICATED);
        }
        entity.setName(name);
        if (request.getSortOrder() != null) {
            entity.setSortOrder(request.getSortOrder());
        }
        categoryMapper.updateById(entity);
        operationLogService.record(operatorId, OperationLogService.MODULE_DICT, "UPDATE_CATEGORY",
                id, Map.of("name", name));
    }

    @Transactional
    public void updateCategoryStatus(Long operatorId, Long id, EnableStatus status) {
        JobCategory entity = categoryMapper.selectById(id);
        if (entity == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "职位类别不存在");
        }
        entity.setStatus(status);
        categoryMapper.updateById(entity);
        operationLogService.record(operatorId, OperationLogService.MODULE_DICT, "STATUS_CATEGORY",
                id, Map.of("status", status.name()));
    }

    /* ---------------- 工作地点写操作 ---------------- */

    @Transactional
    public Long createLocation(Long operatorId, DictItemRequest request) {
        String name = request.getName().trim();
        if (locationMapper.countByNameExcluding(name, -1L) > 0) {
            throw BizException.of(ErrorCode.DICT_NAME_DUPLICATED);
        }
        JobLocation entity = new JobLocation();
        entity.setName(name);
        entity.setSortOrder(request.getSortOrder() == null ? nextLocationSort() : request.getSortOrder());
        entity.setStatus(EnableStatus.ENABLED);
        locationMapper.insert(entity);
        operationLogService.record(operatorId, OperationLogService.MODULE_DICT, "CREATE_LOCATION",
                entity.getId(), Map.of("name", name));
        return entity.getId();
    }

    @Transactional
    public void updateLocation(Long operatorId, Long id, DictItemRequest request) {
        JobLocation entity = locationMapper.selectById(id);
        if (entity == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "工作地点不存在");
        }
        String name = request.getName().trim();
        if (locationMapper.countByNameExcluding(name, id) > 0) {
            throw BizException.of(ErrorCode.DICT_NAME_DUPLICATED);
        }
        entity.setName(name);
        if (request.getSortOrder() != null) {
            entity.setSortOrder(request.getSortOrder());
        }
        locationMapper.updateById(entity);
        operationLogService.record(operatorId, OperationLogService.MODULE_DICT, "UPDATE_LOCATION",
                id, Map.of("name", name));
    }

    @Transactional
    public void updateLocationStatus(Long operatorId, Long id, EnableStatus status) {
        JobLocation entity = locationMapper.selectById(id);
        if (entity == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "工作地点不存在");
        }
        entity.setStatus(status);
        locationMapper.updateById(entity);
        operationLogService.record(operatorId, OperationLogService.MODULE_DICT, "STATUS_LOCATION",
                id, Map.of("status", status.name()));
    }

    /* ---------------- 职位表单校验用 ---------------- */

    public JobCategory requireCategory(Long id) {
        JobCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "职位类别不存在");
        }
        return category;
    }

    public JobLocation requireLocation(Long id) {
        JobLocation location = locationMapper.selectById(id);
        if (location == null) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "工作地点不存在");
        }
        return location;
    }

    /**
     * 新建职位时只能选启用项；编辑时若沿用原有的已停用项则放行（§10.7）。
     */
    public void assertSelectable(JobCategory category, JobLocation location, Long previousCategoryId,
                                 Long previousLocationId) {
        if (category.getStatus() != EnableStatus.ENABLED && !category.getId().equals(previousCategoryId)) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "所选职位类别已停用，请重新选择");
        }
        if (location.getStatus() != EnableStatus.ENABLED && !location.getId().equals(previousLocationId)) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "所选工作地点已停用，请重新选择");
        }
    }

    private int nextCategorySort() {
        return categoryMapper.listAll().stream()
                .map(JobCategory::getSortOrder)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 10;
    }

    private int nextLocationSort() {
        return locationMapper.listAll().stream()
                .map(JobLocation::getSortOrder)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 10;
    }
}
