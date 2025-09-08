package com.tanglinlin.picture.backend.util;

import com.tanglinlin.picture.backend.config.MinioConfig;
import com.tanglinlin.picture.backend.exception.BusinessException;
import com.tanglinlin.picture.backend.exception.ErrorCode;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @program: tanglin-picture-backend
 * @ClassName MinioUtils
 * @description:
 * @author: TSL
 * @create: 2025-09-01 11:05
 * @Version 1.0
 **/
@Slf4j
@Component
public class MinioUtils {
    @Resource
    private MinioConfig minioConfig;

    @Resource
    private MinioClient minioClient;
    /**
     * 查看存储bucket是否存在
     *
     * @param bucketName 存储桶名称
     * @return boolean
     */
    public Boolean bucketExists(String bucketName) {
        Boolean found;
        try {
            found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return found;
    }

    /**
     * 创建存储bucket
     *
     * @param bucketName 存储桶名称
     * @return Boolean
     */
    public Boolean makeBucket(String bucketName) {
        try {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 删除存储bucket
     *
     * @param bucketName 存储桶名称
     * @return Boolean
     */
    public Boolean removeBucket(String bucketName) {
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 获取全部bucket
     *
     * @return 存储桶列表
     */
    public List<Bucket> getAllBuckets() {
        try {
            return minioClient.listBuckets();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 文件上传
     *
     * @param file 文件
     * @return 文件对象名称
     */
    /**
     * 你的原方法：按日期前缀 yyyy/MM/dd 归档
     */
    public String upload(MultipartFile file) {
        return uploadWithPrefix(null, file);
    }

    /**
     * 新增：允许自定义对象名前缀（例如 "test"）
     * 最终对象名：{customPrefix可选}/yyyy/MM/dd/{uuid}.{ext}
     */
    public String upload(String customPrefix, MultipartFile file) {
        return uploadWithPrefix(customPrefix, file);
    }

    private String uploadWithPrefix(String customPrefix, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new RuntimeException("original filename is empty");
        }

        // 扩展名（含点）
        String ext = "";
        int dot = originalFilename.lastIndexOf('.');
        if (dot >= 0) {
            ext = originalFilename.substring(dot);
        }

        // 对象名：可选自定义前缀 + 日期 + uuid
        String datePrefix = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String uuidName = UUID.randomUUID() + ext;

        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(customPrefix)) {
            sb.append(customPrefix.replaceAll("^/+|/+$", "")) // 去首尾斜杠
                    .append('/');
        }
        sb.append(datePrefix).append('/').append(uuidName);
        String objectName = sb.toString();

        try {
            PutObjectArgs objectArgs = PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();

            // 文件名称相同会覆盖（这里我们用了 uuid，几乎不会重名）
            minioClient.putObject(objectArgs);
            return objectName;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"minio putObject failed");
        }
    }

    /**
     * 预览图片
     *
     * @param fileName 文件名称
     * @return 文件预览链接
     */
    public String preview(String fileName) {
        // 查看文件地址
        GetPresignedObjectUrlArgs build = GetPresignedObjectUrlArgs
                .builder()
                .bucket(minioConfig.getBucketName())
                .object(fileName).method(Method.GET).build();
        try {
            String url = minioClient.getPresignedObjectUrl(build);
            return url;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 文件下载
     *
     * @param fileName 文件名称
     * @param res      response
     */
    public void download(String fileName, HttpServletResponse res) {
        GetObjectArgs objectArgs = GetObjectArgs.builder().bucket(minioConfig.getBucketName())
                .object(fileName).build();
        try (GetObjectResponse response = minioClient.getObject(objectArgs)) {
            byte[] buf = new byte[1024];
            int len;
            try (FastByteArrayOutputStream os = new FastByteArrayOutputStream()) {
                while ((len = response.read(buf)) != -1) {
                    os.write(buf, 0, len);
                }
                os.flush();
                byte[] bytes = os.toByteArray();
                res.setCharacterEncoding("utf-8");
                String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
                res.addHeader("Content-Disposition", "attachment;fileName=" + encodedName);
                try (ServletOutputStream stream = res.getOutputStream()) {
                    stream.write(bytes);
                    stream.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 查看文件对象
     *
     * @return 存储bucket内文件对象信息
     */
    public List<Item> listObjects() {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder().bucket(minioConfig.getBucketName()).build());
        List<Item> items = new ArrayList<>();
        try {
            for (Result<Item> result : results) {
                items.add(result.get());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return items;
    }

    /**
     * 删除
     *
     * @param fileName 文件名称
     * @return 是否删除成功
     */
    public boolean remove(String fileName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(fileName)
                    .build());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
