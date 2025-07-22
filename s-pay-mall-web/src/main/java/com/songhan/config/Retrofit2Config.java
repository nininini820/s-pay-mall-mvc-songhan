package com.songhan.config;

import com.songhan.service.weixin.IWeixinApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Slf4j
@Configuration
public class Retrofit2Config {

    private static final String BASE_URL = "https://api.weixin.qq.com/";

    /**
     * 创建并配置一个 Retrofit 实例
     *
     * 使用 Jackson 作为 JSON 转换器（Spring 默认也用 Jackson）
     *
     * 这个 Retrofit 对象之后可以被用于创建各种 HTTP 接口代理对象（类似动态代理）
     * @return
     */
    @Bean
    public Retrofit retrofit() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(JacksonConverterFactory.create()).build();
    }

    /**
     * 使用上面创建的 Retrofit 对象来生成一个 IWeixinApiService 实例
     *
     * 并注入 Spring 容器中，供你后续通过 @Autowired / @Resource 注入使用
     * @param retrofit
     * @return
     */
    @Bean
    public IWeixinApiService weixinApiService(Retrofit retrofit) {
        return retrofit.create(IWeixinApiService.class);
    }

}
