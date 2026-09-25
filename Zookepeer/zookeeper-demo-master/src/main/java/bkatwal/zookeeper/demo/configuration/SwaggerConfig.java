package bkatwal.zookeeper.demo.configuration;

/** @author "Bikas Katwal" 30/03/19 */
import static springfox.documentation.builders.PathSelectors.regex;

import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

  /**
   * Every Docket bean is picked up by the swagger-mvc framework - allowing for multiple swagger
   * groups i.e. same code base multiple swagger resource listings.
   */
  @Bean
  public Docket customDocket() {
    return new Docket(DocumentationType.SWAGGER_2)
        .select()
        .apis(RequestHandlerSelectors.any())
        .paths(regex("/.*"))
        .paths(PathSelectors.regex("(?!/error).+"))
        .paths(PathSelectors.regex("(?!/updateFromLeader).+"))
        .build()
        .apiInfo(metaData());
  }

  private ApiInfo metaData() {
    return new ApiInfo(
        "Distribuirani ML model u Zookeeper klasteru",
        "PUT /model trenira novi model i azurira ga na svim serverima, "
            + "POST /predict vraca predikciju sa bilo kog servera.",
        "v1",
        "Terms of service",
        new Contact("Bikas Katwal", "", "bikas.katwal10@gmail.com"),
        "",
        "",
        Collections.emptyList());
  }
}
