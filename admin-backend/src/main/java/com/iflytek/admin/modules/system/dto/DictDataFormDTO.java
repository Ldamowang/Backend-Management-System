package com.iflytek.admin.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DictDataFormDTO {

    @NotBlank(message = "字典类型不能为空")
    private String dictType;

    @NotBlank(message = "字典标签不能为空")
    @Size(max = 100, message = "字典标签最多100个字符")
    private String dictLabel;

    @NotBlank(message = "字典值不能为空")
    @Size(max = 100, message = "字典值最多100个字符")
    private String dictValue;

    private Integer sortOrder = 0;

    private Integer status = 1;

    @Size(max = 500, message = "描述最多500个字符")
    private String description;
}
