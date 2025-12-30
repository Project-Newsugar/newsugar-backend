package newsugar.Newsugar_Back.api;

import newsugar.Newsugar_Back.common.ApiResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ApiResult<String>> health() {
        return ResponseEntity.ok(ApiResult.ok("ok"));
    }
}

