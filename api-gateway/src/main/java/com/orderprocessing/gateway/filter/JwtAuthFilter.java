package com.orderprocessing.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.public-paths}")
    private List<String> publicPaths;

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        String correlationId = getOrCreateCorrelationId(request);
        ServerHttpRequest mutatedRequest = request.mutate()
            .header("X-Correlation-Id", correlationId)
            .build();

        if (isPublicPath(path)) {
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("[correlationId={}] Missing or malformed Authorization header for path={}", correlationId, path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            Claims claims = parseToken(token);
            String subject = claims.getSubject();

            mutatedRequest = mutatedRequest.mutate()
                .header("X-User-Id", subject)
                .build();

            log.debug("[correlationId={}] JWT validated for subject={} path={}", correlationId, subject, path);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (JwtException ex) {
            log.warn("[correlationId={}] Invalid JWT for path={}: {}", correlationId, path, ex.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private String getOrCreateCorrelationId(ServerHttpRequest request) {
        String existing = request.getHeaders().getFirst("X-Correlation-Id");
        return (existing != null && !existing.isBlank()) ? existing : UUID.randomUUID().toString();
    }

    private boolean isPublicPath(String path) {
        return publicPaths.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }
}
