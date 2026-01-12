package newsugar.Newsugar_Back.config;

import com.amazonaws.xray.jakarta.servlet.AWSXRayServletFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.Filter;

// AWS X-Ray 설정 파일입니다. 들어오는 요청들 필터링해서 추적 시작하는 곳입니다.
@Configuration
public class XRayConfig {

    // 필터 등록하는 겁니다. 이거 있어야 요청 들어올 때 X-Ray가 낚아챕니다.
    @Bean
    public Filter TracingFilter() {
        return new AWSXRayServletFilter("Newsugar-Backend");
    }
}
