package com.ssafy.eatBusan.config;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.DefaultUriBuilderFactory.EncodingMode;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    @Value("${data.api.baseurl}")
    private final String baseUrl;

    @Bean
    public RestClient dataClient(){
        return RestClient.builder()
                .requestFactory(requestFactory())
                .uriBuilderFactory(uriBuilderFactory())
                .defaultStatusHandler(
                        HttpStatusCode::is4xxClientError,
                        (req, res) -> log.info("4xxClientError")
                )
                .defaultStatusHandler(
                        HttpStatusCode::is5xxServerError,
                        (req, res) -> log.info("5xxServerError")
                )
                .build();
    }

    @Bean
    public SimpleClientHttpRequestFactory requestFactory(){
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(60L));
        requestFactory.setConnectTimeout(Duration.ofSeconds(60L));
        return requestFactory;
    }

    @Bean
    public DefaultUriBuilderFactory uriBuilderFactory(){
        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory(baseUrl);
        uriBuilderFactory.setEncodingMode(EncodingMode.NONE);
        return uriBuilderFactory;
    }

}
