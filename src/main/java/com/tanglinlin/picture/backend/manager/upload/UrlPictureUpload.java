package com.tanglinlin.picture.backend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.tanglinlin.picture.backend.exception.BusinessException;
import com.tanglinlin.picture.backend.exception.ErrorCode;
import com.tanglinlin.picture.backend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * @program: tanglin-picture-backend
 * @ClassName UrlPictureUpload
 * @description:
 * @author: TSL
 * @create: 2025-09-08 14:18
 * @Version 1.0
 **/
@Service
public class UrlPictureUpload extends PictureUploadTemplate {
    @Override
    protected void validPicture(Object inputSource) {
        String url = (String) inputSource;
        ThrowUtils.throwIf(StrUtil.isBlank(url),
                ErrorCode.PARAMS_ERROR, "图片地址为空");
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
                ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "上传图片地址不存在");
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

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String url = (String) inputSource;
        HttpUtil.downloadFile(url, file);
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        String url = (String) inputSource;
        HttpResponse response = null;
        String extend = "";
        try {
            response = HttpUtil.createRequest(Method.HEAD, url).execute();
            if (response.getStatus() != HttpStatus.HTTP_OK)
                ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "上传图片地址不存在");
            //4.校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                //允许的图片类型
                List<String> allowImageList = Arrays.asList("image/jpeg", "image/ipg", "image/png", "image/webp");
                ThrowUtils.throwIf(!allowImageList.contains(contentType), ErrorCode.PARAMS_ERROR, "上传图片格式不支持");
            }
            extend = contentType.split("/")[1];
        } finally {
            if (ObjectUtil.isNotNull(response)) {
                response.close();
            }
        }
        return FileUtil.mainName(url) + "." + extend;
    }
}
