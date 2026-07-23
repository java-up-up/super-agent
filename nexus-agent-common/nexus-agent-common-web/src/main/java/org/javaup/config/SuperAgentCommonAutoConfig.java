package org.javaup.config;

import java.util.List;

import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 自动配置类
 * @author: 阿星不是程序员
 **/

public class SuperAgentCommonAutoConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustom() {
        return new JacksonCustom();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(WebMvcConfigurer.class)
    @ConditionalOnBean(ObjectMapper.class)
    public WebMvcConfigurer webMvcJacksonConfigurer(ObjectMapper objectMapper) {
        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {

                MappingJackson2HttpMessageConverter mvcJacksonConverter =
                    new MappingJackson2HttpMessageConverter(createMvcObjectMapper(objectMapper));

                for (int i = 0; i < converters.size(); i++) {
                    if (converters.get(i) instanceof MappingJackson2HttpMessageConverter existingConverter) {

                        mvcJacksonConverter.setSupportedMediaTypes(existingConverter.getSupportedMediaTypes());
                        converters.set(i, mvcJacksonConverter);
                        return;
                    }
                }

                converters.add(0, mvcJacksonConverter);
            }
        };
    }

    private ObjectMapper createMvcObjectMapper(ObjectMapper objectMapper) {
        ObjectMapper mvcObjectMapper = objectMapper.copy();
        mvcObjectMapper.setSerializerFactory(
            mvcObjectMapper.getSerializerFactory().withSerializerModifier(new JsonCustomSerializer())
        );
        mvcObjectMapper.getFactory().configure(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS.mappedFeature(), true);
        return mvcObjectMapper;
    }
}
