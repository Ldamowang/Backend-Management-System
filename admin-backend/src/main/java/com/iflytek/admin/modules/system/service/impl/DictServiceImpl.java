package com.iflytek.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.admin.common.constant.CacheConstants;
import com.iflytek.admin.common.exception.BusinessException;
import com.iflytek.admin.common.service.CacheService;
import com.iflytek.admin.modules.system.dto.DictDataFormDTO;
import com.iflytek.admin.modules.system.dto.DictTypeFormDTO;
import com.iflytek.admin.modules.system.entity.SysDictData;
import com.iflytek.admin.modules.system.entity.SysDictType;
import com.iflytek.admin.modules.system.mapper.SysDictDataMapper;
import com.iflytek.admin.modules.system.mapper.SysDictTypeMapper;
import com.iflytek.admin.modules.system.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictServiceImpl implements DictService {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;
    private final CacheService cacheService;

    @Override
    public List<SysDictType> listTypes() {
        List<SysDictType> cached = cacheService.getAsList(CacheConstants.DICT_TYPES_KEY);
        if (cached != null) return cached;
        List<SysDictType> types = dictTypeMapper.selectList(new LambdaQueryWrapper<SysDictType>()
                .orderByDesc(SysDictType::getCreatedTime));
        cacheService.set(CacheConstants.DICT_TYPES_KEY, types, 86400);
        return types;
    }

    @Override
    public SysDictType getTypeById(Long id) {
        SysDictType type = dictTypeMapper.selectById(id);
        if (type == null) throw new BusinessException(404, "字典类型不存在");
        return type;
    }

    @Override
    public void createType(DictTypeFormDTO dto) {
        Long count = dictTypeMapper.selectCount(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictType, dto.getDictType()));
        if (count > 0) throw new BusinessException(400, "字典类型已存在");

        SysDictType type = new SysDictType();
        type.setDictName(dto.getDictName());
        type.setDictType(dto.getDictType());
        type.setStatus(dto.getStatus());
        type.setDescription(dto.getDescription());
        dictTypeMapper.insert(type);
        cacheService.delete(CacheConstants.DICT_TYPES_KEY);
    }

    @Override
    public void updateType(Long id, DictTypeFormDTO dto) {
        SysDictType type = dictTypeMapper.selectById(id);
        if (type == null) throw new BusinessException(404, "字典类型不存在");

        String oldDictType = type.getDictType();
        if (dto.getDictName() != null) type.setDictName(dto.getDictName());
        if (dto.getDictType() != null) type.setDictType(dto.getDictType());
        if (dto.getStatus() != null) type.setStatus(dto.getStatus());
        if (dto.getDescription() != null) type.setDescription(dto.getDescription());
        dictTypeMapper.updateById(type);

        // 同步更新字典数据的 dict_type
        if (dto.getDictType() != null && !oldDictType.equals(dto.getDictType())) {
            SysDictData update = new SysDictData();
            update.setDictType(dto.getDictType());
            dictDataMapper.update(update, new LambdaQueryWrapper<SysDictData>()
                    .eq(SysDictData::getDictType, oldDictType));
            cacheService.delete(CacheConstants.DICT_DATA_PREFIX + dto.getDictType());
        }
        cacheService.delete(CacheConstants.DICT_TYPES_KEY);
        cacheService.delete(CacheConstants.DICT_DATA_PREFIX + oldDictType);
    }

    @Override
    @Transactional
    public void deleteType(Long id) {
        SysDictType type = dictTypeMapper.selectById(id);
        if (type == null) throw new BusinessException(404, "字典类型不存在");

        dictTypeMapper.deleteById(id);
        dictDataMapper.delete(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, type.getDictType()));
        cacheService.delete(CacheConstants.DICT_TYPES_KEY);
        cacheService.delete(CacheConstants.DICT_DATA_PREFIX + type.getDictType());
    }

    @Override
    public List<SysDictData> listDataByType(String dictType) {
        String key = CacheConstants.DICT_DATA_PREFIX + dictType;
        List<SysDictData> cached = cacheService.getAsList(key);
        if (cached != null) return cached;
        List<SysDictData> data = dictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType)
                .orderByAsc(SysDictData::getSortOrder));
        cacheService.set(key, data, 86400);
        return data;
    }

    @Override
    public SysDictData getDataById(Long id) {
        SysDictData data = dictDataMapper.selectById(id);
        if (data == null) throw new BusinessException(404, "字典数据不存在");
        return data;
    }

    @Override
    public void createData(DictDataFormDTO dto) {
        SysDictData data = new SysDictData();
        data.setDictType(dto.getDictType());
        data.setDictLabel(dto.getDictLabel());
        data.setDictValue(dto.getDictValue());
        data.setSortOrder(dto.getSortOrder());
        data.setStatus(dto.getStatus());
        data.setDescription(dto.getDescription());
        dictDataMapper.insert(data);
        cacheService.delete(CacheConstants.DICT_DATA_PREFIX + dto.getDictType());
    }

    @Override
    public void updateData(Long id, DictDataFormDTO dto) {
        SysDictData data = dictDataMapper.selectById(id);
        if (data == null) throw new BusinessException(404, "字典数据不存在");

        if (dto.getDictLabel() != null) data.setDictLabel(dto.getDictLabel());
        if (dto.getDictValue() != null) data.setDictValue(dto.getDictValue());
        if (dto.getSortOrder() != null) data.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) data.setStatus(dto.getStatus());
        if (dto.getDescription() != null) data.setDescription(dto.getDescription());
        dictDataMapper.updateById(data);
        cacheService.delete(CacheConstants.DICT_DATA_PREFIX + data.getDictType());
    }

    @Override
    public void deleteData(Long id) {
        SysDictData data = dictDataMapper.selectById(id);
        dictDataMapper.deleteById(id);
        if (data != null) {
            cacheService.delete(CacheConstants.DICT_DATA_PREFIX + data.getDictType());
        }
    }
}
