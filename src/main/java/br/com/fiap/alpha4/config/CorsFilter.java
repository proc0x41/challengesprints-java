package br.com.fiap.alpha4.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter extends OncePerRequestFilter {
    private final Set<String> origins;

    public CorsFilter(@Value("${app.cors-origins}") String origins) {
        this.origins = Arrays.stream(origins.split(",")).map(String::trim)
            .filter(origin -> !origin.isEmpty()).collect(Collectors.toSet());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        response.addHeader("Vary", "Origin");
        if (origin != null) {
            if (!origins.contains(origin)) {
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"Origem não autorizada.\"}");
                return;
            }
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Admin-Key");
            response.setHeader("Access-Control-Max-Age", "3600");
        }
        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatus(204);
            return;
        }
        chain.doFilter(request, response);
    }
}
