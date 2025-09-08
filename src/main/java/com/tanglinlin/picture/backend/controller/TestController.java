package com.tanglinlin.picture.backend.controller;

import cn.hutool.core.util.ObjectUtil;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.tanglinlin.picture.backend.annotation.AuthCheck;
import com.tanglinlin.picture.backend.common.BaseResponse;
import com.tanglinlin.picture.backend.common.ResultUtils;
import com.tanglinlin.picture.backend.exception.BusinessException;
import com.tanglinlin.picture.backend.exception.ErrorCode;
import com.tanglinlin.picture.backend.manager.CosManager;
import com.tanglinlin.picture.backend.util.MinioUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * @Author:TSL
 * @Date: 2025/8/28  00:30
 * @Description
 */
@RestController
@Slf4j
public class TestController {
    @Autowired
    private CosManager cosManager;

    @Operation(summary = "健康检查,返回ok")
    @GetMapping("/health")
    public String health() {
        return "ok";
    }

    @Operation(summary = "健康检查,返回ok,加上参数")
    @Parameters({
            @Parameter(name = "id", description = "测试id", in = ParameterIn.QUERY)
    })
    @GetMapping("/healthWithId")
    public String health(@RequestParam("id") String id) {
        return "ok" + id;
    }

    //    @Resource
//    private MinioUtils minioUtils;
//
//    @PostMapping("/test/upload")
//    @AuthCheck(mustRole = "admin")
//    public BaseResponse<?> testUpload(@RequestPart("file") MultipartFile multipartFile) {
//        if (multipartFile == null || multipartFile.isEmpty()) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件为空");
//        }
//        // 可选：限制文件类型或大小
//        if (!StringUtils.hasText(multipartFile.getOriginalFilename())) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名为空");
//        }
//
//        try {
//            // 统一放到 test/ 目录下
//            String objectName = minioUtils.upload("test", multipartFile);
//            return ResultUtils.success(objectName);
//        } catch (Exception e) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
//        }
//    }
//
//    @PostMapping("/test/download")
//    @AuthCheck(mustRole = "admin")
//    @Operation(summary = "文件下载", description = "文件下载")
//    public BaseResponse<?> testDownload(@RequestParam("fileName") String fileName, HttpServletResponse httpServletResponse) {
//        if (!StringUtils.hasText(fileName)) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名为空");
//        }
//        try {
//            minioUtils.download(fileName, httpServletResponse);
//            return ResultUtils.success("下载成功");
//        } catch (Exception e) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件下载失败");
//        }
//    }
    @PostMapping("/test/upload")
    @AuthCheck(mustRole = "admin")
    @Operation(summary = "文件上传", description = "文件上传")
    public BaseResponse<?> testUpload(@RequestPart("file") MultipartFile multipartFile) {
        String filename = multipartFile.getOriginalFilename();
        if (!StringUtils.hasText(filename)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名为空");
        }
        String filepath = String.format("/test/%s", filename);
        File file = null;
        try {
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filepath, file);
            return ResultUtils.success(filepath);
        } catch (IOException e) {
            log.error("file upload filed ,filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        } finally {
            if (ObjectUtil.isNotNull(file)) {
                if (!file.delete()) {
                    log.error("file delete filed ,filepath = " + filepath);
                }
            }
        }

    }

    @GetMapping("/test/download")
    @AuthCheck(mustRole = "admin")
    @Operation(summary = "文件下载", description = "文件下载")
    public void testDownload(@RequestParam("fileName") String filepath, HttpServletResponse httpServletResponse) throws IOException {
        COSObjectInputStream cosObjectInputStream = null;
        try {
            COSObject object = cosManager.getObject(filepath);
            cosObjectInputStream = object.getObjectContent();
            //处理下载的流
            byte[] byteArray = IOUtils.toByteArray(cosObjectInputStream);
            //设置响应头
            httpServletResponse.setContentType("application/octet-stream;charset=UTF-8");
            httpServletResponse.setHeader("Content-Disposition", "attachment;filename=" + filepath);
            //写入响应
            httpServletResponse.getOutputStream().write(byteArray);
            httpServletResponse.getOutputStream().flush();
        } catch (IOException e) {
            log.error("file download filed ,filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件下载失败");
        } finally {
            if (ObjectUtil.isNotNull(cosObjectInputStream)) {
                cosObjectInputStream.close();
            }
        }
    }

}

