package org.gwh.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate配置类
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 创建RestTemplate Bean
     * @return RestTemplate实例
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(getClientHttpRequestFactory());
    }

    /**
     * 配置ClientHttpRequestFactory
     * @return ClientHttpRequestFactory实例
     */
    private ClientHttpRequestFactory getClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 设置连接超时时间(ms)
        factory.setConnectTimeout(60000);
        // 设置读取超时时间(ms)
        factory.setReadTimeout(60000);
        return factory;
    }
} 