package com.tanglinlin.picture.backend.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @program: tanglin-picture-backend
 * @ClassName CosClientConfig
 * @description:
 * @author: TSL
 * @create: 2025-09-06 14:18
 * @Version 1.0
 **/
@Configuration
@Data
@ConfigurationProperties(prefix = "cos.client")
public class CosClientConfig {
    /**
     * 域名
     */
    private String host;
    /**
     * 密钥id
     */
    private String secretId;
    /**
     * 密钥key
     */

    private String secretKey;
    /**
     * 区域
     */
    private String region;
    /**
     * 桶名
     */
    private String bucket;

    @Bean
    public COSClient cosClient() {
        COSCredentials basicCOSCredentials = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        return new COSClient(basicCOSCredentials, clientConfig);
    }


}
