package com.nttdata.banking.account.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

@Configuration
public class OpenAPIConfig {
    @Value("${nttdata.openapi.dev-url}")
    private String devUrl;
    @Value("${nttdata.openapi.prod-url}")
    private String prodUrl;

    @Bean
    public OpenAPI openAPI() {
        Server devServer = new Server();
        devServer.setUrl(devUrl);
        devServer.setDescription("Development server");
        Server prodServer = new Server();
        prodServer.setUrl(prodUrl);
        prodServer.setDescription("Production server");

        Contact contact = new Contact();
        contact.setName("Nttdata Banking");
        contact.setEmail("nttdata@nttdata.com");
        contact.setUrl("https://nttdata.com");

        License mirLicense = new License().name("MIT").url("https://opensource.org/licenses/MIT");

        Info info = new Info();
        info.setTitle("Banking API");
        info.setDescription("API for managing banking accounts and transactions");
        info.setVersion("1.0.0");
        info.setContact(contact);
        info.setLicense(mirLicense);
        info.termsOfService("https://nttdata.com/terms-of-service");

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer, prodServer));
    }

}
