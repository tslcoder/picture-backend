package com.tanglinlin.picture.backend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import com.tanglinlin.picture.backend.exception.ErrorCode;
import com.tanglinlin.picture.backend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @program: tanglin-picture-backend
 * @ClassName FilePictureUpload
 * @description:
 * @author: TSL
 * @create: 2025-09-08 14:10
 * @Version 1.0
 **/
@Service
public class FilePictureUpload extends PictureUploadTemplate{

    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(ObjectUtil.isNull(multipartFile),
                ErrorCode.PARAMS_ERROR,"上传文件为空");
        //1.校验文件大小
        Long fileSize = multipartFile.getSize();
        final Long TWO_MB = 1024 * 1024 * 2L;
        ThrowUtils.throwIf(fileSize > TWO_MB,
                ErrorCode.PARAMS_ERROR,"上传文件过大,不能超过2MB");
        //2.校验文件后缀
        String suffix = FileUtil.getSuffix(this.getOriginalFilename(inputSource));
        //允许上传的文件后缀
        final List<String> allowUploadSuffixArray = Arrays.asList("jpg", "jpeg", "png", "gif") ;
        ThrowUtils.throwIf(!allowUploadSuffixArray.contains(suffix),
                ErrorCode.PARAMS_ERROR,"上传图片格式不支持");
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);

    }
}
