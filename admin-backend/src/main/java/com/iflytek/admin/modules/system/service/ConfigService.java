package com.iflytek.admin.modules.system.service;

import com.iflytek.admin.modules.system.entity.SysConfig;

import java.util.List;

public interface ConfigService {
    List<SysConfig> listAll();
    String getValueByKey(String key);
    void batchUpdate(List<SysConfig> configs);
}
