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
        // X-Ray 세그먼트 없으면 그냥 넘깁니다. 스케줄러 같은 애들이 컨텍스트 없이 들어오면 에러 터지니까 막아둔 겁니다.
        if (!AWSXRay.getCurrentSegmentOptional().isPresent()) {
            return pjp.proceed();
        }

        // 여기서 서브세그먼트 엽니다. 이제부터 이 메소드 끝날 때까지 시간 잽니다.
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
