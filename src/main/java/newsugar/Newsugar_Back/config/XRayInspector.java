package newsugar.Newsugar_Back.config;

import com.amazonaws.xray.spring.aop.AbstractXRayInterceptor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class XRayInspector extends AbstractXRayInterceptor {

    @Override
    @Pointcut("execution(* newsugar.Newsugar_Back.domain..service..*(..)) || execution(* newsugar.Newsugar_Back.domain..repository..*(..))")
    public void xrayEnabledClasses() {}
}
