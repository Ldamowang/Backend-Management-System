package com.iflytek.admin.modules.system.service;

import com.iflytek.admin.common.result.PageResult;
import com.iflytek.admin.modules.system.entity.SysFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface FileService {
    SysFile upload(MultipartFile file);
    Resource download(Long id);
    PageResult<SysFile> page(int page, int size);
    void delete(Long id);
}
