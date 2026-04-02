package com.iflytek.admin.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DictTypeFormDTO {

    @NotBlank(message = "字典名称不能为空")
    @Size(max = 100, message = "字典名称最多100个字符")
    private String dictName;

    @NotBlank(message = "字典类型不能为空")
    @Size(max = 100, message = "字典类型最多100个字符")
    private String dictType;

    private Integer status = 1;

    @Size(max = 500, message = "描述最多500个字符")
    private String description;
}
