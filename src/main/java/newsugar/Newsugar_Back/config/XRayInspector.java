package newsugar.Newsugar_Back.config;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class XRayInspector {

    @Pointcut("execution(* newsugar.Newsugar_Back.domain..service..*(..)) || execution(* newsugar.Newsugar_Back.domain..repository..*(..))")
    public void xrayEnabledClasses() {}

    @Around("xrayEnabledClasses()")
    public Object traceMethod(ProceedingJoinPoint pjp) throws Throwable {
        // X-Ray 세그먼트가 없으면 추적하지 않음 (스케줄러 등 컨텍스트 유실 시 에러 방지)
        if (!AWSXRay.getCurrentSegmentOptional().isPresent()) {
            return pjp.proceed();
        }

        // 서브세그먼트 생성
        Subsegment subsegment = AWSXRay.beginSubsegment(pjp.getSignature().getName());
        try {
            if (subsegment != null) {
                subsegment.putMetadata("Class", pjp.getTarget().getClass().getSimpleName());
                subsegment.putMetadata("Method", pjp.getSignature().getName());
            }
            return pjp.proceed();
        } catch (Exception e) {
            if (subsegment != null) {
                subsegment.addException(e);
            }
            throw e;
        } finally {
            if (subsegment != null) {
                AWSXRay.endSubsegment();
            }
        }
    }
}
