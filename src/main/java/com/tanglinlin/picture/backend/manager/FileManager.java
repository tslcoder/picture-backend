package com.tanglinlin.picture.backend.manager;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
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
@Deprecated
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
                deleteTempFile(file);
            }
        }
    }

    /**
     * 上传文件格式校验
     *
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
        if (!isDelete) {
            log.error("delete temp file failed, filePath= " + file.getAbsolutePath());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除临时文件失败");
        }
    }

    /**
     * 通过url上传图片
     * @param url
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPictureByUrl(String url,
                                                  String uploadPathPrefix) {
        //校验url
        validPictureUrl(url);
        //图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = FileUtil.mainName(url);
        String uploadFileName = String.format("%s_%s.%s",
                DateUtil.format(new Date(), "yyyyMMdd"), uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFileName);
        File file = null;
        try {
            file = File.createTempFile(uploadPath, null);
//            multipartFile.transferTo(file);
            HttpUtil.downloadFile(url, file);
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
                deleteTempFile(file);
            }
        }
    }

    private void validPictureUrl(String url) {
        ThrowUtils.throwIf(ObjectUtil.isNull(url), ErrorCode.PARAMS_ERROR, "上传图片地址为空");
        try {
            //1.验证URL格式
            new URL(url);
        } catch (MalformedURLException e) {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "上传图片地址格式错误");
        }
        //2.校验URL协议
        ThrowUtils.throwIf(!url.startsWith("http://") && !url.startsWith("https://"),
                ErrorCode.PARAMS_ERROR, "仅支持HTTP或者HTTPS的文件地址");
        //3.发送HEAD请求来验证文件是否存在
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, url).execute();
            if (response.getStatus() != HttpStatus.HTTP_OK)
                return;
            //4.校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                //允许的图片类型
                List<String> allowImageList = Arrays.asList("image/jpeg", "image/ipg", "image/png", "image/webp");
                ThrowUtils.throwIf(!allowImageList.contains(contentType), ErrorCode.PARAMS_ERROR, "上传图片格式不支持");
            }
            //5.校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLeng = Long.parseLong(contentLengthStr);
                    final long TWO_MB = 2 * 1024 * 1024L;
                    ThrowUtils.throwIf(contentLeng > TWO_MB, ErrorCode.PARAMS_ERROR, "上传图片过大,不能超过2MB");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传图片大小异常");
                }
            }
        } finally {
            if (ObjectUtil.isNotNull(response)) {
                response.close();
            }
        }
    }


}
