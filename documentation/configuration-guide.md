# Globeco Trade Service: Configuration and Monitoring Guide

## Overview

This guide covers the configuration and monitoring setup for the enhanced Globeco Trade Service with v2 API capabilities, external service integration, and caching infrastructure.

## Configuration Properties

### Application Configuration

#### `application.yml`
```yaml
# Application Configuration
spring:
  application:
    name: globeco-trade-service
  
  # Database Configuration
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/globeco_trade}
    username: ${DATABASE_USERNAME:trade_user}
    password: ${DATABASE_PASSWORD:trade_password}
    hikari:
      maximum-pool-size: ${DB_POOL_SIZE:20}
      minimum-idle: ${DB_POOL_MIN_IDLE:5}
      connection-timeout: ${DB_CONNECTION_TIMEOUT:30000}
      idle-timeout: ${DB_IDLE_TIMEOUT:600000}
      max-lifetime: ${DB_MAX_LIFETIME:1800000}
  
  # JPA Configuration
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: ${SHOW_SQL:false}
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          batch_size: 25
        order_inserts: true
        order_updates: true
  
  # Cache Configuration
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=5m

# External Service Configuration
external-services:
  security-service:
    base-url: ${SECURITY_SERVICE_URL:http://globeco-security-service:8000}
    timeout: ${SECURITY_SERVICE_TIMEOUT:5000}
    retry:
      max-attempts: ${SECURITY_SERVICE_RETRY_ATTEMPTS:3}
      delay: ${SECURITY_SERVICE_RETRY_DELAY:1000}
  
  portfolio-service:
    base-url: ${PORTFOLIO_SERVICE_URL:http://globeco-portfolio-service:8000}
    timeout: ${PORTFOLIO_SERVICE_TIMEOUT:5000}
    retry:
      max-attempts: ${PORTFOLIO_SERVICE_RETRY_ATTEMPTS:3}
      delay: ${PORTFOLIO_SERVICE_RETRY_DELAY:1000}

# Cache Configuration
cache:
  security:
    max-size: ${SECURITY_CACHE_MAX_SIZE:1000}
    expire-after-write: ${SECURITY_CACHE_TTL:5m}
    record-stats: true
  
  portfolio:
    max-size: ${PORTFOLIO_CACHE_MAX_SIZE:1000}
    expire-after-write: ${PORTFOLIO_CACHE_TTL:5m}
    record-stats: true

# API Configuration
api:
  pagination:
    default-page-size: ${DEFAULT_PAGE_SIZE:20}
    max-page-size: ${MAX_PAGE_SIZE:1000}
  
  batch:
    max-size: ${BATCH_MAX_SIZE:100}
    timeout: ${BATCH_TIMEOUT:30000}

# Monitoring Configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,caches
      base-path: /actuator
  
  endpoint:
    health:
      show-details: always
      show-components: always
    
    metrics:
      enabled: true
  
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: globeco-trade-service
      environment: ${ENVIRONMENT:local}

# Logging Configuration
logging:
  level:
    org.kasbench.globeco_trade_service: ${LOG_LEVEL:INFO}
    org.springframework.web: ${WEB_LOG_LEVEL:WARN}
    org.hibernate.SQL: ${SQL_LOG_LEVEL:WARN}
    org.hibernate.type.descriptor.sql.BasicBinder: ${SQL_PARAMS_LOG_LEVEL:WARN}
  
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  
  file:
    name: ${LOG_FILE:logs/globeco-trade-service.log}
    max-size: ${LOG_MAX_SIZE:10MB}
    max-history: ${LOG_MAX_HISTORY:30}
```

#### Environment-Specific Configurations

##### `application-local.yml`
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/globeco_trade
  
  jpa:
    show-sql: true

external-services:
  security-service:
    base-url: http://localhost:8001
  portfolio-service:
    base-url: http://localhost:8002

logging:
  level:
    org.kasbench.globeco_trade_service: DEBUG
```

##### `application-staging.yml`
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    hikari:
      maximum-pool-size: 15

external-services:
  security-service:
    base-url: https://security-service-staging.globeco.internal
  portfolio-service:
    base-url: https://portfolio-service-staging.globeco.internal

logging:
  level:
    org.kasbench.globeco_trade_service: INFO
```

##### `application-production.yml`
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10

external-services:
  security-service:
    base-url: https://security-service.globeco.internal
  portfolio-service:
    base-url: https://portfolio-service.globeco.internal

logging:
  level:
    org.kasbench.globeco_trade_service: WARN
    org.springframework.web: ERROR
```

### Docker Configuration

#### `docker-compose.yml`
```yaml
version: '3.8'

services:
  trade-service:
    image: globeco/trade-service:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DATABASE_URL=jdbc:postgresql://postgres:5432/globeco_trade
      - DATABASE_USERNAME=trade_user
      - DATABASE_PASSWORD=trade_password
      - SECURITY_SERVICE_URL=http://security-service:8000
      - PORTFOLIO_SERVICE_URL=http://portfolio-service:8000
      - ENVIRONMENT=docker
    depends_on:
      - postgres
      - security-service
      - portfolio-service
    networks:
      - globeco-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  postgres:
    image: postgres:15
    environment:
      - POSTGRES_DB=globeco_trade
      - POSTGRES_USER=trade_user
      - POSTGRES_PASSWORD=trade_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./trade-service.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - globeco-network

  security-service:
    image: globeco/security-service:latest
    ports:
      - "8001:8000"
    networks:
      - globeco-network

  portfolio-service:
    image: globeco/portfolio-service:latest
    ports:
      - "8002:8000"
    networks:
      - globeco-network

volumes:
  postgres_data:

networks:
  globeco-network:
    driver: bridge
```

### Kubernetes Configuration

#### `k8s-configmap.yaml`
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: trade-service-config
  namespace: globeco
data:
  application.yml: |
    spring:
      cache:
        type: caffeine
        caffeine:
          spec: maximumSize=1000,expireAfterWrite=5m
    
    external-services:
      security-service:
        base-url: http://security-service:8000
        timeout: 5000
        retry:
          max-attempts: 3
          delay: 1000
      portfolio-service:
        base-url: http://portfolio-service:8000
        timeout: 5000
        retry:
          max-attempts: 3
          delay: 1000
    
    api:
      pagination:
        default-page-size: 20
        max-page-size: 1000
      batch:
        max-size: 100
        timeout: 30000
    
    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus,caches
    
    logging:
      level:
        org.kasbench.globeco_trade_service: INFO
```

#### `k8s-secret.yaml`
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: trade-service-secrets
  namespace: globeco
type: Opaque
stringData:
  DATABASE_URL: "jdbc:postgresql://postgres-service:5432/globeco_trade"
  DATABASE_USERNAME: "trade_user"
  DATABASE_PASSWORD: "secure_password_here"
```

## Monitoring Setup

### Metrics Configuration

#### Custom Metrics
```java
// Application Metrics Configuration
@Configuration
public class MetricsConfig {
    
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags("application", "globeco-trade-service")
            .commonTags("environment", environment);
    }
    
    // Custom Counters
    @Bean
    public Counter apiRequestCounter(MeterRegistry registry) {
        return Counter.builder("api.requests.total")
            .description("Total API requests")
            .tag("version", "v2")
            .register(registry);
    }
    
    @Bean
    public Timer apiRequestTimer(MeterRegistry registry) {
        return Timer.builder("api.requests.duration")
            .description("API request duration")
            .register(registry);
    }
    
    // External Service Metrics
    @Bean
    public Counter externalServiceCallsCounter(MeterRegistry registry) {
        return Counter.builder("external.service.calls.total")
            .description("Total external service calls")
            .register(registry);
    }
    
    @Bean
    public Timer externalServiceTimer(MeterRegistry registry) {
        return Timer.builder("external.service.duration")
            .description("External service call duration")
            .register(registry);
    }
    
    // Cache Metrics
    @Bean
    public Gauge cacheHitRateGauge(MeterRegistry registry, SecurityCacheService securityCache) {
        return Gauge.builder("cache.hit.rate")
            .description("Cache hit rate")
            .tag("cache", "security")
            .register(registry, securityCache, service -> service.getHitRate());
    }
}
```

### Prometheus Configuration

#### `prometheus.yml`
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "trade-service-alerts.yml"

scrape_configs:
  - job_name: 'trade-service'
    static_configs:
      - targets: ['trade-service:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    scrape_timeout: 5s

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093
```

#### Alert Rules (`trade-service-alerts.yml`)
```yaml
groups:
  - name: trade-service-alerts
    rules:
      # High Error Rate
      - alert: TradeServiceHighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
        for: 2m
        labels:
          severity: critical
          service: trade-service
        annotations:
          summary: "High error rate in Trade Service"
          description: "Error rate is {{ $value }} requests per second"
      
      # High Response Time
      - alert: TradeServiceHighLatency
        expr: http_server_requests_seconds{quantile="0.95"} > 2
        for: 5m
        labels:
          severity: warning
          service: trade-service
        annotations:
          summary: "High latency in Trade Service"
          description: "95th percentile latency is {{ $value }} seconds"
      
      # External Service Failures
      - alert: ExternalServiceFailures
        expr: rate(external_service_calls_total{status="failure"}[5m]) > 0.05
        for: 2m
        labels:
          severity: warning
          service: trade-service
        annotations:
          summary: "External service failures detected"
          description: "Failure rate is {{ $value }} per second"
      
      # Low Cache Hit Rate
      - alert: LowCacheHitRate
        expr: cache_hit_rate < 0.7
        for: 10m
        labels:
          severity: warning
          service: trade-service
        annotations:
          summary: "Low cache hit rate"
          description: "Cache hit rate is {{ $value }}"
      
      # Database Connection Pool Exhaustion
      - alert: DatabasePoolExhaustion
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 1m
        labels:
          severity: critical
          service: trade-service
        annotations:
          summary: "Database connection pool near exhaustion"
          description: "{{ $value }}% of connections in use"
```

### Grafana Dashboards

#### Trade Service Dashboard JSON
```json
{
  "dashboard": {
    "id": null,
    "title": "Globeco Trade Service v2",
    "tags": ["globeco", "trade-service"],
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "API Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count[5m])",
            "legendFormat": "{{method}} {{uri}}"
          }
        ],
        "yAxes": [
          {
            "label": "Requests/sec"
          }
        ]
      },
      {
        "id": 2,
        "title": "API Response Times",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))",
            "legendFormat": "95th percentile"
          },
          {
            "expr": "histogram_quantile(0.50, rate(http_server_requests_seconds_bucket[5m]))",
            "legendFormat": "50th percentile"
          }
        ]
      },
      {
        "id": 3,
        "title": "Cache Performance",
        "type": "graph",
        "targets": [
          {
            "expr": "cache_hit_rate",
            "legendFormat": "{{cache}} hit rate"
          }
        ]
      },
      {
        "id": 4,
        "title": "External Service Calls",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(external_service_calls_total[5m])",
            "legendFormat": "{{service}} {{status}}"
          }
        ]
      },
      {
        "id": 5,
        "title": "Database Connections",
        "type": "graph",
        "targets": [
          {
            "expr": "hikaricp_connections_active",
            "legendFormat": "Active"
          },
          {
            "expr": "hikaricp_connections_idle",
            "legendFormat": "Idle"
          }
        ]
      }
    ],
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "refresh": "5s"
  }
}
```

## Health Checks

### Custom Health Indicators

#### External Service Health Check
```java
@Component
public class ExternalServiceHealthIndicator implements HealthIndicator {
    
    private final SecurityServiceClient securityServiceClient;
    private final PortfolioServiceClient portfolioServiceClient;
    
    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        
        // Check Security Service
        try {
            securityServiceClient.healthCheck();
            builder.withDetail("security-service", "UP");
        } catch (Exception e) {
            builder.withDetail("security-service", "DOWN")
                   .withDetail("security-service-error", e.getMessage());
        }
        
        // Check Portfolio Service
        try {
            portfolioServiceClient.healthCheck();
            builder.withDetail("portfolio-service", "UP");
        } catch (Exception e) {
            builder.withDetail("portfolio-service", "DOWN")
                   .withDetail("portfolio-service-error", e.getMessage());
        }
        
        return builder.up().build();
    }
}
```

#### Cache Health Check
```java
@Component
public class CacheHealthIndicator implements HealthIndicator {
    
    private final SecurityCacheService securityCacheService;
    private final PortfolioCacheService portfolioCacheService;
    
    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        
        // Security Cache Stats
        CacheStats securityStats = securityCacheService.getStats();
        builder.withDetail("security-cache-hit-rate", securityStats.hitRate())
               .withDetail("security-cache-size", securityCacheService.size());
        
        // Portfolio Cache Stats
        CacheStats portfolioStats = portfolioCacheService.getStats();
        builder.withDetail("portfolio-cache-hit-rate", portfolioStats.hitRate())
               .withDetail("portfolio-cache-size", portfolioCacheService.size());
        
        return builder.up().build();
    }
}
```

### Health Check Endpoints

#### Readiness Probe
```yaml
# k8s-deployment.yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
```

#### Liveness Probe
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 30
  timeoutSeconds: 10
  failureThreshold: 3
```

## Logging Configuration

### Structured Logging

#### Logback Configuration (`logback-spring.xml`)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <!-- Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
                <stackTrace/>
            </providers>
        </encoder>
    </appender>
    
    <!-- File Appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/globeco-trade-service.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/globeco-trade-service.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
            <maxFileSize>10MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
                <stackTrace/>
            </providers>
        </encoder>
    </appender>
    
    <!-- Async Appenders -->
    <appender name="ASYNC_CONSOLE" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="CONSOLE"/>
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
    </appender>
    
    <appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="FILE"/>
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
    </appender>
    
    <!-- Logger Configuration -->
    <logger name="org.kasbench.globeco_trade_service" level="INFO"/>
    <logger name="org.springframework.web" level="WARN"/>
    <logger name="org.hibernate.SQL" level="WARN"/>
    
    <root level="INFO">
        <appender-ref ref="ASYNC_CONSOLE"/>
        <appender-ref ref="ASYNC_FILE"/>
    </root>
</configuration>
```

## Performance Tuning

### JVM Configuration
```bash
# Production JVM Settings
JAVA_OPTS="-Xms2g -Xmx4g \
           -XX:+UseG1GC \
           -XX:MaxGCPauseMillis=200 \
           -XX:+UseStringDeduplication \
           -XX:+ExitOnOutOfMemoryError \
           -Djava.security.egd=file:/dev/./urandom"
```

### Database Optimization
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
  
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 25
          fetch_size: 50
        order_inserts: true
        order_updates: true
        connection:
          provider_disables_autocommit: true
```

### Cache Optimization
```yaml
cache:
  security:
    max-size: 2000
    expire-after-write: 10m
    refresh-after-write: 8m
  
  portfolio:
    max-size: 1000
    expire-after-write: 15m
    refresh-after-write: 12m
```

## Troubleshooting

### Common Issues

#### 1. High Memory Usage
**Symptoms**: OutOfMemoryError, high heap usage
**Solutions**:
- Increase JVM heap size
- Tune cache sizes
- Check for memory leaks in external service clients

#### 2. Slow Database Queries
**Symptoms**: High response times, database timeouts
**Solutions**:
- Add database indexes for common filter combinations
- Optimize HikariCP settings
- Use database query profiling

#### 3. External Service Timeouts
**Symptoms**: Frequent external service failures
**Solutions**:
- Increase timeout values
- Implement circuit breaker pattern
- Check network connectivity

#### 4. Low Cache Hit Rates
**Symptoms**: High external service call volume
**Solutions**:
- Increase cache TTL values
- Optimize cache key strategies
- Pre-warm cache on startup

### Monitoring Commands
```bash
# Check application status
curl http://localhost:8080/actuator/health

# View metrics
curl http://localhost:8080/actuator/metrics

# Cache statistics
curl http://localhost:8080/actuator/caches

# Application info
curl http://localhost:8080/actuator/info

# Thread dump
curl http://localhost:8080/actuator/threaddump

# Heap dump
curl http://localhost:8080/actuator/heapdump
```

## Security Configuration

### SSL/TLS Configuration
```yaml
server:
  ssl:
    enabled: true
    key-store: ${SSL_KEYSTORE_PATH}
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    protocol: TLS
    enabled-protocols: TLSv1.2,TLSv1.3
```

### Security Headers
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .headers(headers -> headers
                .contentTypeOptions(ContentTypeOptionsHeaderWriter.builder().build())
                .frameOptions(FrameOptionsHeaderWriter.XFrameOptionsMode.DENY)
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true))
            )
            .build();
    }
} 