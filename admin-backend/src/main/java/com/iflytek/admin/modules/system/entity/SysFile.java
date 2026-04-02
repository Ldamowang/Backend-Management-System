package com.iflytek.admin.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_file")
public class SysFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileName;
    private String storedName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private String fileExt;
    private String uploader;
    private LocalDateTime createdTime;
}
