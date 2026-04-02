package com.iflytek.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.admin.common.exception.BusinessException;
import com.iflytek.admin.modules.system.dto.DeptFormDTO;
import com.iflytek.admin.modules.system.entity.SysDepartment;
import com.iflytek.admin.modules.system.mapper.SysDepartmentMapper;
import com.iflytek.admin.modules.system.service.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    private final SysDepartmentMapper deptMapper;

    @Override
    public List<Map<String, Object>> getDeptTree() {
        List<SysDepartment> depts = deptMapper.selectList(
                new LambdaQueryWrapper<SysDepartment>().orderByAsc(SysDepartment::getSortOrder));
        return buildTree(depts, 0L);
    }

    @Override
    public List<Map<String, Object>> getDeptSimpleTree() {
        List<SysDepartment> depts = deptMapper.selectList(
                new LambdaQueryWrapper<SysDepartment>()
                        .eq(SysDepartment::getStatus, 1)
                        .orderByAsc(SysDepartment::getSortOrder));
        return buildSimpleTree(depts, 0L);
    }

    @Override
    public SysDepartment getById(Long id) {
        SysDepartment dept = deptMapper.selectById(id);
        if (dept == null) throw new BusinessException(404, "部门不存在");
        return dept;
    }

    @Override
    public void create(DeptFormDTO dto) {
        if (dto.getParentId() != 0) {
            SysDepartment parent = deptMapper.selectById(dto.getParentId());
            if (parent == null) throw new BusinessException(400, "父部门不存在");
        }
        SysDepartment dept = new SysDepartment();
        applyDTO(dept, dto);
        deptMapper.insert(dept);
    }

    @Override
    public void update(Long id, DeptFormDTO dto) {
        SysDepartment dept = deptMapper.selectById(id);
        if (dept == null) throw new BusinessException(404, "部门不存在");
        if (id.equals(dto.getParentId())) throw new BusinessException(400, "父部门不能是自身");
        applyDTO(dept, dto);
        deptMapper.updateById(dept);
    }

    @Override
    public void delete(Long id) {
        Long count = deptMapper.selectCount(
                new LambdaQueryWrapper<SysDepartment>().eq(SysDepartment::getParentId, id));
        if (count > 0) throw new BusinessException(400, "存在子部门，无法删除");
        deptMapper.deleteById(id);
    }

    private void applyDTO(SysDepartment dept, DeptFormDTO dto) {
        dept.setParentId(dto.getParentId());
        dept.setDeptName(dto.getDeptName());
        dept.setSortOrder(dto.getSortOrder());
        dept.setLeader(dto.getLeader());
        dept.setPhone(dto.getPhone());
        dept.setEmail(dto.getEmail());
        dept.setStatus(dto.getStatus());
    }

    private List<Map<String, Object>> buildTree(List<SysDepartment> depts, Long parentId) {
        return depts.stream()
                .filter(d -> Objects.equals(d.getParentId(), parentId))
                .map(d -> {
                    Map<String, Object> node = new LinkedHashMap<>();
                    node.put("id", d.getId());
                    node.put("parentId", d.getParentId());
                    node.put("deptName", d.getDeptName());
                    node.put("sortOrder", d.getSortOrder());
                    node.put("leader", d.getLeader());
                    node.put("phone", d.getPhone());
                    node.put("email", d.getEmail());
                    node.put("status", d.getStatus());
                    node.put("createdTime", d.getCreatedTime());
                    node.put("children", buildTree(depts, d.getId()));
                    return node;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildSimpleTree(List<SysDepartment> depts, Long parentId) {
        return depts.stream()
                .filter(d -> Objects.equals(d.getParentId(), parentId))
                .map(d -> {
                    Map<String, Object> node = new LinkedHashMap<>();
                    node.put("id", d.getId());
                    node.put("deptName", d.getDeptName());
                    node.put("children", buildSimpleTree(depts, d.getId()));
                    return node;
                })
                .collect(Collectors.toList());
    }
}
