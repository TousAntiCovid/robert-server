package fr.gouv.clea.ws.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.List;

import static springfox.documentation.builders.RequestHandlerSelectors.basePackage;

@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

    @Bean
    public Docket tacwdApi() {
        return new Docket(DocumentationType.OAS_30)
                .select()
                .apis(basePackage("fr.gouv.clea.ws"))
                .paths(PathSelectors.regex("/api/.*"))
                .build()
                .apiInfo(
                        new ApiInfo(
                                "Tous AntiCovid Cluster Exposure Verification (Cléa)",
                                "#TOUSANTICOVID, TAC Warning Dynamic API",
                                "1.0.0",
                                null,
                                new Contact(null, null, "stopcovid@inria.fr"),
                                "Apache 2.0",
                                "http://www.apache.org/licenses/LICENSE-2.0.html",
                                List.of()
                        )
                )
                .groupName("tacwd")
                .globalResponses(HttpMethod.GET, List.of())
                .globalResponses(HttpMethod.POST, List.of());
    }
}
