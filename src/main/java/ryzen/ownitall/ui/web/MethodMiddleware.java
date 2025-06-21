package ryzen.ownitall.ui.web;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MethodMiddleware implements HandlerInterceptor {
    private static final Logger logger = new Logger(MethodMiddleware.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (MethodMenu.getMethod() == null) {
            logger.info("method not initialized, redirecting...");
            response.sendRedirect("/method?callback=" + request.getRequestURI());
            return false;
        } else {
            logger.debug("current method '" + MethodMenu.getMethod().getMethodName() + "'");
        }
        return true;
    }
}
