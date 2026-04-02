package com.iflytek.admin.modules.system.controller;

import com.iflytek.admin.common.annotation.Idempotent;
import com.iflytek.admin.common.annotation.Log;
import com.iflytek.admin.common.result.Result;
import com.iflytek.admin.modules.system.dto.DictDataFormDTO;
import com.iflytek.admin.modules.system.dto.DictTypeFormDTO;
import com.iflytek.admin.modules.system.entity.SysDictData;
import com.iflytek.admin.modules.system.entity.SysDictType;
import com.iflytek.admin.modules.system.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "字典管理")
@RestController
@RequestMapping("/api/dicts")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    // ========== 字典类型 ==========

    @Operation(summary = "字典类型列表")
    @PreAuthorize("hasAuthority('sys:dict:list')")
    @GetMapping("/types")
    public Result<List<SysDictType>> listTypes() {
        return Result.ok(dictService.listTypes());
    }

    @Operation(summary = "字典类型详情")
    @PreAuthorize("hasAuthority('sys:dict:list')")
    @GetMapping("/types/{id}")
    public Result<SysDictType> getTypeById(@PathVariable Long id) {
        return Result.ok(dictService.getTypeById(id));
    }

    @Operation(summary = "新增字典类型")
    @PreAuthorize("hasAuthority('sys:dict:add')")
    @Log(module = "字典管理", operation = "新增类型")
    @Idempotent
    @PostMapping("/types")
    public Result<Void> createType(@Valid @RequestBody DictTypeFormDTO dto) {
        dictService.createType(dto);
        return Result.ok();
    }

    @Operation(summary = "编辑字典类型")
    @PreAuthorize("hasAuthority('sys:dict:edit')")
    @Log(module = "字典管理", operation = "编辑类型")
    @PutMapping("/types/{id}")
    public Result<Void> updateType(@PathVariable Long id, @Valid @RequestBody DictTypeFormDTO dto) {
        dictService.updateType(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除字典类型")
    @PreAuthorize("hasAuthority('sys:dict:delete')")
    @Log(module = "字典管理", operation = "删除类型")
    @DeleteMapping("/types/{id}")
    public Result<Void> deleteType(@PathVariable Long id) {
        dictService.deleteType(id);
        return Result.ok();
    }

    // ========== 字典数据 ==========

    @Operation(summary = "根据字典类型查询数据")
    @GetMapping("/data/{dictType}")
    public Result<List<SysDictData>> listDataByType(@PathVariable String dictType) {
        return Result.ok(dictService.listDataByType(dictType));
    }

    @Operation(summary = "字典数据详情")
    @PreAuthorize("hasAuthority('sys:dict:list')")
    @GetMapping("/data/detail/{id}")
    public Result<SysDictData> getDataById(@PathVariable Long id) {
        return Result.ok(dictService.getDataById(id));
    }

    @Operation(summary = "新增字典数据")
    @PreAuthorize("hasAuthority('sys:dict:add')")
    @Log(module = "字典管理", operation = "新增数据")
    @Idempotent
    @PostMapping("/data")
    public Result<Void> createData(@Valid @RequestBody DictDataFormDTO dto) {
        dictService.createData(dto);
        return Result.ok();
    }

    @Operation(summary = "编辑字典数据")
    @PreAuthorize("hasAuthority('sys:dict:edit')")
    @Log(module = "字典管理", operation = "编辑数据")
    @PutMapping("/data/{id}")
    public Result<Void> updateData(@PathVariable Long id, @Valid @RequestBody DictDataFormDTO dto) {
        dictService.updateData(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除字典数据")
    @PreAuthorize("hasAuthority('sys:dict:delete')")
    @Log(module = "字典管理", operation = "删除数据")
    @DeleteMapping("/data/{id}")
    public Result<Void> deleteData(@PathVariable Long id) {
        dictService.deleteData(id);
        return Result.ok();
    }
}
