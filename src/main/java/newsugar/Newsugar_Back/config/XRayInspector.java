package newsugar.Newsugar_Back.config;

import com.amazonaws.xray.spring.aop.AbstractXRayInterceptor;
import com.amazonaws.xray.AWSXRay;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class XRayInspector extends AbstractXRayInterceptor {

    @Override
    @Pointcut("execution(* newsugar.Newsugar_Back.domain..service..*(..)) || execution(* newsugar.Newsugar_Back.domain..repository..*(..))")
    public void xrayEnabledClasses() {}

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 현재 스레드에 X-Ray 세그먼트가 있을 때만 하위 세그먼트 생성 (스케줄러 등 컨텍스트 유실 시 에러 방지)
        if (AWSXRay.getCurrentSegmentOptional().isPresent()) {
            return super.invoke(invocation);
        }
        return invocation.proceed();
    }
}
