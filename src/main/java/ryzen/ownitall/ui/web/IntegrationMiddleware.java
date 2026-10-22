package ryzen.ownitall.ui.web;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class IntegrationMiddleware implements HandlerInterceptor {
    private static final Logger logger = new Logger(IntegrationMiddleware.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (IntegrationMenu.getIntegration() == null) {
            logger.info("integration not initialized, redirecting...");
            response.sendRedirect("/integration?callback=" + request.getRequestURI());
            return false;
        } else {
            logger.debug("current integration '" + IntegrationMenu.getIntegration().getIntegrationName() + "'");
        }
        return true;
    }
}
