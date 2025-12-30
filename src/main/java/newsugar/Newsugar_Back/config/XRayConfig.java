package newsugar.Newsugar_Back.config;

import com.amazonaws.xray.jakarta.servlet.AWSXRayServletFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.Filter;

@Configuration
public class XRayConfig {

    @Bean
    public Filter TracingFilter() {
        return new AWSXRayServletFilter("Newsugar-Backend");
    }
}
