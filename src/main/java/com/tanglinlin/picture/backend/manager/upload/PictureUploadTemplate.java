package com.tanglinlin.picture.backend.manager.upload;

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
import com.tanglinlin.picture.backend.manager.CosManager;
import com.tanglinlin.picture.backend.model.dto.file.UploadPictureResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Date;

/**
 * 设计模式：模版方法模式
 * @program: tanglin-picture-backend
 * @ClassName PictureUploadTemplate
 * @description:
 * @author: TSL
 * @create: 2025-09-08 11:18
 * @Version 1.0
 **/
@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private CosManager cosManager;

    /**
     * 模版方法，定义上传流程
     */
    public final UploadPictureResult uploadPicture(Object inputSource,
                                                   String uploadPathPrefix) {
        //1.校验图片
        validPicture(inputSource);

        //2。图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginalFilename(inputSource);
        String uploadFileName = String.format("%s_%s.%s",
                DateUtil.format(new Date(), "yyyyMMdd"), uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFileName);

        File file = null;
        try {
            //3.创建临时文件
            file = File.createTempFile(uploadPath, null);
            processFile(inputSource, file);
            //4.上传图片到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //5.封装返回结果
            return buildResult(originalFilename, file, uploadPath, imageInfo);
        } catch (Exception e) {
            log.error("上传图片失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传图片失败");
        } finally {
            if (file != null) {
                deleteTempFile(file);
            }
        }
    }

    /**
     * 校验数据源（本地文件或者url）
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取原始文件名
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 封装返回结果
     */
    private UploadPictureResult buildResult(String originalFilename,
                                            File file,
                                            String uploadPath,
                                            ImageInfo imageInfo) {
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
    }

    /**
     * 删除临时文件
     */
    public void deleteTempFile(File file) {
        if (ObjectUtil.isNull(file)) {
            return;
        }
        boolean isDelete = file.delete();
        if (!isDelete) {
            log.error("delete temp file failed, filePath= " + file.getAbsolutePath());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除临时文件失败");
        }
    }
}
