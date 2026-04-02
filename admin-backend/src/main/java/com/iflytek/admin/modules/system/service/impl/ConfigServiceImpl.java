package com.iflytek.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.admin.common.constant.CacheConstants;
import com.iflytek.admin.common.service.CacheService;
import com.iflytek.admin.modules.system.entity.SysConfig;
import com.iflytek.admin.modules.system.mapper.SysConfigMapper;
import com.iflytek.admin.modules.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService {

    private final SysConfigMapper configMapper;
    private final CacheService cacheService;

    @Override
    public List<SysConfig> listAll() {
        List<SysConfig> cached = cacheService.getAsList(CacheConstants.CONFIG_ALL_KEY);
        if (cached != null) return cached;
        List<SysConfig> configs = configMapper.selectList(null);
        cacheService.set(CacheConstants.CONFIG_ALL_KEY, configs, CacheConstants.CONFIG_TTL);
        return configs;
    }

    @Override
    public String getValueByKey(String key) {
        String cacheKey = CacheConstants.CONFIG_PREFIX + key;
        String cached = cacheService.get(cacheKey, String.class);
        if (cached != null) return cached;
        SysConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        String value = config != null ? config.getConfigValue() : null;
        if (value != null) {
            cacheService.set(cacheKey, value, CacheConstants.CONFIG_TTL);
        }
        return value;
    }

    @Override
    @Transactional
    public void batchUpdate(List<SysConfig> configs) {
        configs.forEach(configMapper::updateById);
        cacheService.delete(CacheConstants.CONFIG_ALL_KEY);
        cacheService.deleteByPrefix(CacheConstants.CONFIG_PREFIX);
    }
}
