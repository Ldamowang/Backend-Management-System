package com.iflytek.admin.modules.system.service;

import com.iflytek.admin.modules.system.dto.DictDataFormDTO;
import com.iflytek.admin.modules.system.dto.DictTypeFormDTO;
import com.iflytek.admin.modules.system.entity.SysDictData;
import com.iflytek.admin.modules.system.entity.SysDictType;

import java.util.List;

public interface DictService {
    List<SysDictType> listTypes();
    SysDictType getTypeById(Long id);
    void createType(DictTypeFormDTO dto);
    void updateType(Long id, DictTypeFormDTO dto);
    void deleteType(Long id);

    List<SysDictData> listDataByType(String dictType);
    SysDictData getDataById(Long id);
    void createData(DictDataFormDTO dto);
    void updateData(Long id, DictDataFormDTO dto);
    void deleteData(Long id);
}
