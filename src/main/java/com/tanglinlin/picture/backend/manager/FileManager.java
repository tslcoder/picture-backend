package com.tanglinlin.picture.backend.manager;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.tanglinlin.picture.backend.config.CosClientConfig;
import com.tanglinlin.picture.backend.exception.BusinessException;
import com.tanglinlin.picture.backend.exception.ErrorCode;
import com.tanglinlin.picture.backend.exception.ThrowUtils;
import com.tanglinlin.picture.backend.model.dto.file.UploadPictureResult;
import jakarta.annotation.Resource;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @program: tanglin-picture-backend
 * @ClassName FileManager
 * @description:
 * @author: TSL
 * @create: 2025-09-06 17:03
 * @Version 1.0
 **/
@Component
@Data
public class FileManager {
    private static final Logger log = LoggerFactory.getLogger(FileManager.class);
    @Resource
    private CosManager cosManager;
    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 上传图片
     */

    public UploadPictureResult uploadPicture(MultipartFile multipartFile,
                                             String uploadPathPrefix) {
        //校验图片
        validPicture(multipartFile);
        //图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        String uploadFileName = String.format("%s_%s.%s",
                DateUtil.format(new Date(), "yyyyMMdd"), uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFileName);
        File file = null;
        try {
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            //上传图片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //todo 为什么getCiUploadResult为空 已解决
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            return uploadPictureResult;
        } catch (IOException e) {
            log.error("upload picture to cos_store filed", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        } finally {
            if (ObjectUtil.isNotNull(file)) {
                file.delete();
            }
        }
    }

    /**
     * 上传文件格式校验
     * @param file
     */
    public void validPicture(MultipartFile file) {
        ThrowUtils.throwIf(ObjectUtil.isNull(file), ErrorCode.PARAMS_ERROR, "上传图片为空");
        //1.检验图片大小
        long size = file.getSize();
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIf(size > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "上传图片过大,不能超过两MB");
        //2.检验图片文件后缀
        String suffix = FileUtil.getSuffix(file.getOriginalFilename());
        // 允许上传的后缀
        final List<String> allowUploadSuffixList = List.of("jpg", "jpeg", "png", "gif");
        ThrowUtils.throwIf(!allowUploadSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "上传图片格式不支持");
    }
    /**
     * 删除临时文件
     */
    public void deleteTempFile(File file) {
        if (ObjectUtil.isNull(file)) {
            return;
        }
        boolean isDelete = file.delete();
        if (!isDelete){
            log.error("delete temp file failed, filePath= " + file.getAbsolutePath());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除临时文件失败");
        }
    }
}
