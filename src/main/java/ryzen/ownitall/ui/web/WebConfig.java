package ryzen.ownitall.ui.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private IntegrationMiddleware integrationMiddleware;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(
                this.integrationMiddleware).addPathPatterns("/integration/import/**", "/integration/export/**",
                        "/integration/sync/**", "/integration/discoveries/**");
    }
}
