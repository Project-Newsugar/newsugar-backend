package newsugar.Newsugar_Back.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class NewsApiConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}