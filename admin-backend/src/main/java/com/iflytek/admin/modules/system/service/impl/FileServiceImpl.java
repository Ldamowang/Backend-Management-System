package com.iflytek.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iflytek.admin.common.exception.BusinessException;
import com.iflytek.admin.common.result.PageResult;
import com.iflytek.admin.common.utils.SecurityUtil;
import com.iflytek.admin.modules.system.entity.SysFile;
import com.iflytek.admin.modules.system.mapper.SysFileMapper;
import com.iflytek.admin.modules.system.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final SysFileMapper fileMapper;

    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    @Value("${app.upload.max-size:10485760}")
    private long maxFileSize;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "csv", "zip", "rar", "7z"
    );

    @Override
    public SysFile upload(MultipartFile file) {
        if (file.isEmpty()) throw new BusinessException(400, "文件不能为空");
        if (file.getSize() > maxFileSize) throw new BusinessException(400, "文件大小超过限制");

        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new BusinessException(400, "不支持的文件类型: " + ext);
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dirPath = Paths.get(uploadPath, datePath);

        try {
            Files.createDirectories(dirPath);
            Path targetPath = dirPath.resolve(storedName);
            file.transferTo(targetPath.toFile());

            SysFile sysFile = new SysFile();
            sysFile.setFileName(originalName);
            sysFile.setStoredName(storedName);
            sysFile.setFilePath(datePath + "/" + storedName);
            sysFile.setFileSize(file.getSize());
            sysFile.setFileType(file.getContentType());
            sysFile.setFileExt(ext);
            sysFile.setUploader(SecurityUtil.getCurrentUsername());
            fileMapper.insert(sysFile);
            return sysFile;
        } catch (IOException e) {
            throw new BusinessException(500, "文件上传失败");
        }
    }

    @Override
    public Resource download(Long id) {
        SysFile sysFile = fileMapper.selectById(id);
        if (sysFile == null) throw new BusinessException(404, "文件不存在");

        try {
            Path filePath = Paths.get(uploadPath).resolve(sysFile.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) throw new BusinessException(404, "文件不存在");
            return resource;
        } catch (IOException e) {
            throw new BusinessException(500, "文件下载失败");
        }
    }

    @Override
    public PageResult<SysFile> page(int page, int size) {
        Page<SysFile> result = fileMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<SysFile>().orderByDesc(SysFile::getCreatedTime));
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public void delete(Long id) {
        SysFile sysFile = fileMapper.selectById(id);
        if (sysFile == null) throw new BusinessException(404, "文件不存在");

        Path filePath = Paths.get(uploadPath).resolve(sysFile.getFilePath());
        try { Files.deleteIfExists(filePath); } catch (IOException ignored) {}
        fileMapper.deleteById(id);
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}
