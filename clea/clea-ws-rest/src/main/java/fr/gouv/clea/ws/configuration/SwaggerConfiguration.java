package fr.gouv.clea.ws.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.List;

import static springfox.documentation.builders.RequestHandlerSelectors.basePackage;

@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

    private ApiKey apiKey() {
        return new ApiKey(HttpHeaders.AUTHORIZATION, HttpHeaders.AUTHORIZATION, "header");
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder().securityReferences(defaultAuth()).build();
    }

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        return List.of(new SecurityReference(HttpHeaders.AUTHORIZATION, authorizationScopes));
    }

    @Bean
    public Docket cleaApi() {
        return new Docket(DocumentationType.OAS_30)
                .select()
                .apis(basePackage("fr.gouv.clea.ws"))
                .paths(PathSelectors.regex("/api/.*"))
                .build()
                .apiInfo(
                        new ApiInfo(
                                "Tous AntiCovid Cluster Exposure Verification (Cléa)",
                                "#TOUSANTICOVID, Cléa API",
                                "1.0.0",
                                null,
                                new Contact(null, null, "stopcovid@inria.fr"),
                                "Apache 2.0",
                                "http://www.apache.org/licenses/LICENSE-2.0.html",
                                List.of()
                        )
                )
                .groupName("clea")
                .securityContexts(List.of(securityContext()))
                .securitySchemes(List.of(apiKey()))
                .globalResponses(HttpMethod.GET, List.of())
                .globalResponses(HttpMethod.POST, List.of());
    }
}
