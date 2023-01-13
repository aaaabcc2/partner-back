package com.partner.boot.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;


/**
 * <p>
 * 访问路径：http://localhost:9090/swagger-ui/index.html
 * </p>
 */
@Configuration
public class SwaggerConfig extends WebMvcConfigurationSupport {


    @Bean
    public Docket createRestAPI() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.partner.boot.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("RestAPI接口文档")
                .description("Restful 后台接口汇总")
                .contact(new Contact("啊啊啊啊不吵吵", "https://5w.fit/fogXs", "xqnode@163.com"))
                .version("1.0")
                .build();
    }

}