# Globeco Trade Service: Operational Runbook

## Overview

This runbook provides step-by-step procedures for operating, monitoring, and troubleshooting the Globeco Trade Service in production environments.

## Service Architecture

### Service Dependencies
- **Database**: PostgreSQL 13+ (primary data store)
- **External Services**:
  - Security Service (port 8000) - provides ticker symbols
  - Portfolio Service (port 8000) - provides portfolio names
- **Monitoring**: Prometheus, Grafana, AlertManager
- **Load Balancer**: Kubernetes Ingress or external LB

### Service Topology
```
External LB → Kubernetes Ingress → Trade Service Pods → PostgreSQL
                                       ↓
                              External Services (Security, Portfolio)
```

## Deployment Procedures

### Pre-Deployment Checklist

#### Environment Validation
- [ ] PostgreSQL database is accessible
- [ ] External services (Security, Portfolio) are operational
- [ ] Kubernetes cluster has sufficient resources
- [ ] Configuration secrets are properly set
- [ ] Monitoring infrastructure is ready

#### Configuration Review
```bash
# Verify configuration
kubectl get configmaps trade-service-config -o yaml
kubectl get secrets trade-service-secrets -o yaml

# Check resource quotas
kubectl describe quota --namespace=globeco
```

### Deployment Steps

#### 1. Database Migration (if required)
```bash
# Connect to database
kubectl exec -it postgres-pod -- psql -U trade_user -d globeco_trade

# Run migration scripts
\i /migrations/V2_0_0__add_indexes.sql

# Verify schema
\d+ trade_orders
\d+ executions
```

#### 2. Application Deployment
```bash
# Deploy configuration
kubectl apply -f k8s-configmap.yaml
kubectl apply -f k8s-secret.yaml

# Deploy application
kubectl apply -f k8s-deployment.yaml

# Verify deployment
kubectl get pods -l app=trade-service
kubectl logs -f deployment/trade-service
```

#### 3. Health Check Validation
```bash
# Check readiness
kubectl get pods -l app=trade-service
curl -f http://trade-service:8080/actuator/health/readiness

# Check liveness
curl -f http://trade-service:8080/actuator/health/liveness

# Verify external service connectivity
curl -f http://trade-service:8080/actuator/health
```

#### 4. Smoke Testing
```bash
# Test v1 endpoints
curl "http://trade-service:8080/api/v1/tradeOrders?limit=1"

# Test v2 endpoints
curl "http://trade-service:8080/api/v2/tradeOrders?page=0&size=1"

# Test batch operations
curl -X POST "http://trade-service:8080/api/v1/tradeOrders/batch/submit" \
  -H "Content-Type: application/json" \
  -d '{"tradeOrders":[{"orderId":999,"orderType":"BUY","quantity":1,"portfolioId":"TEST","securityId":"TEST","blotterId":"1"}]}'
```

### Post-Deployment Verification

#### Performance Baseline
```bash
# Check response times
curl -w "@curl-format.txt" "http://trade-service:8080/api/v2/tradeOrders"

# Verify cache hit rates
curl "http://trade-service:8080/actuator/metrics/cache.gets"

# Check external service metrics
curl "http://trade-service:8080/actuator/metrics/external.service.calls"
```

#### Monitoring Setup
```bash
# Verify Prometheus scraping
curl "http://prometheus:9090/api/v1/targets"

# Check Grafana dashboards
curl "http://grafana:3000/api/health"

# Validate alerts
curl "http://alertmanager:9093/api/v1/alerts"
```

## Monitoring and Alerting

### Key Metrics to Monitor

#### Application Metrics
| Metric | Threshold | Action |
|--------|-----------|--------|
| Response Time P95 | > 1000ms | Investigate performance |
| Error Rate | > 5% | Check logs and external services |
| Cache Hit Rate | < 70% | Review cache configuration |
| Active Database Connections | > 80% of pool | Scale or investigate leaks |

#### System Metrics
| Metric | Threshold | Action |
|--------|-----------|--------|
| CPU Usage | > 80% | Scale horizontally |
| Memory Usage | > 85% | Investigate memory leaks |
| Disk Usage | > 90% | Clean logs or expand storage |
| Network Latency | > 100ms | Check network infrastructure |

### Dashboard URLs
- **Application Dashboard**: `http://grafana:3000/d/trade-service/trade-service-overview`
- **JVM Dashboard**: `http://grafana:3000/d/jvm/jvm-metrics`
- **Database Dashboard**: `http://grafana:3000/d/postgres/postgresql-overview`

### Alert Escalation

#### Severity Levels
1. **Critical**: Service down, data corruption, security breach
2. **High**: Performance degradation, partial outage
3. **Medium**: Non-critical feature issues, warning thresholds
4. **Low**: Information alerts, maintenance windows

#### Escalation Matrix
| Severity | Immediate Response | Escalation (15 min) | Manager (30 min) |
|----------|-------------------|-------------------|------------------|
| Critical | On-call Engineer | Team Lead | Service Owner |
| High | On-call Engineer | Team Lead | - |
| Medium | On-call Engineer | - | - |
| Low | Ticket Queue | - | - |

## Troubleshooting Procedures

### Common Issues

#### 1. High Response Times

**Symptoms:**
- API response times > 1000ms
- User complaints about slow performance
- High CPU usage

**Investigation Steps:**
```bash
# Check current response times
curl "http://trade-service:8080/actuator/metrics/http.server.requests"

# Review thread dumps
curl "http://trade-service:8080/actuator/threaddump" > threaddump.json

# Check database connections
curl "http://trade-service:8080/actuator/metrics/hikaricp.connections.active"

# Analyze slow queries
kubectl exec -it postgres-pod -- psql -U trade_user -d globeco_trade -c "
SELECT query, mean_time, calls, total_time 
FROM pg_stat_statements 
ORDER BY mean_time DESC 
LIMIT 10;"
```

**Resolution Steps:**
1. Scale application horizontally if CPU bound
2. Optimize database queries if DB bound
3. Increase cache TTL if external service bound
4. Review and tune JVM settings

#### 2. External Service Failures

**Symptoms:**
- Increased error rates
- Cache miss spikes
- Circuit breaker activation

**Investigation Steps:**
```bash
# Check external service health
curl "http://security-service:8000/health"
curl "http://portfolio-service:8000/health"

# Review circuit breaker status
curl "http://trade-service:8080/actuator/circuitbreakers"

# Check external service metrics
curl "http://trade-service:8080/actuator/metrics/external.service.calls"

# Review application logs
kubectl logs -l app=trade-service --tail=100 | grep -i "external\|timeout\|circuit"
```

**Resolution Steps:**
1. Verify external service availability
2. Check network connectivity
3. Review and adjust timeout settings
4. Consider graceful degradation mode

#### 3. Database Connection Pool Exhaustion

**Symptoms:**
- Connection timeout errors
- High database connection usage
- Application hanging

**Investigation Steps:**
```bash
# Check connection pool status
curl "http://trade-service:8080/actuator/metrics/hikaricp.connections"

# Review database locks
kubectl exec -it postgres-pod -- psql -U trade_user -d globeco_trade -c "
SELECT pid, state, query_start, query 
FROM pg_stat_activity 
WHERE state != 'idle' 
ORDER BY query_start;"

# Check for long-running transactions
kubectl exec -it postgres-pod -- psql -U trade_user -d globeco_trade -c "
SELECT pid, now() - xact_start as duration, state, query 
FROM pg_stat_activity 
WHERE xact_start IS NOT NULL 
ORDER BY duration DESC;"
```

**Resolution Steps:**
1. Increase connection pool size if needed
2. Identify and terminate long-running queries
3. Review application connection handling
4. Consider read replicas for read-heavy operations

#### 4. Memory Leaks

**Symptoms:**
- Increasing memory usage over time
- OutOfMemoryError exceptions
- Frequent garbage collection

**Investigation Steps:**
```bash
# Check memory metrics
curl "http://trade-service:8080/actuator/metrics/jvm.memory.used"

# Generate heap dump
curl -X POST "http://trade-service:8080/actuator/heapdump"

# Check GC activity
curl "http://trade-service:8080/actuator/metrics/jvm.gc.pause"

# Review cache sizes
curl "http://trade-service:8080/actuator/caches"
```

**Resolution Steps:**
1. Analyze heap dump with MAT or similar tool
2. Review cache configuration and sizes
3. Check for resource leaks in external service clients
4. Tune JVM garbage collection settings

### Emergency Procedures

#### Service Outage Response

**Immediate Actions (0-5 minutes):**
1. Acknowledge the alert
2. Check service status: `kubectl get pods -l app=trade-service`
3. Review recent deployments: `kubectl rollout history deployment/trade-service`
4. Check infrastructure status (database, external services)

**Assessment Phase (5-15 minutes):**
1. Determine impact scope (partial vs. complete outage)
2. Check error rates and logs
3. Verify external dependencies
4. Communicate status to stakeholders

**Resolution Phase (15+ minutes):**
1. If recent deployment caused issue: `kubectl rollout undo deployment/trade-service`
2. If database issue: Contact DBA team
3. If external service issue: Contact respective service teams
4. If infrastructure issue: Contact platform team

#### Disaster Recovery

**Database Failover:**
```bash
# Switch to backup database
kubectl patch deployment trade-service -p '{"spec":{"template":{"spec":{"containers":[{"name":"trade-service","env":[{"name":"DATABASE_URL","value":"jdbc:postgresql://backup-db:5432/globeco_trade"}]}]}}}}'

# Verify connectivity
kubectl exec -it trade-service-pod -- pg_isready -h backup-db
```

**Cross-Region Failover:**
```bash
# Update DNS to point to secondary region
# This would typically be handled by infrastructure team

# Verify service health in secondary region
curl "http://trade-service-dr:8080/actuator/health"
```

## Maintenance Procedures

### Scheduled Maintenance

#### Database Maintenance
```bash
# Schedule during off-peak hours (typically 2-4 AM)

# 1. Create database backup
kubectl exec postgres-pod -- pg_dump -U trade_user globeco_trade > backup.sql

# 2. Update connection pool to minimum
kubectl patch deployment trade-service --type='merge' -p='{"spec":{"template":{"spec":{"containers":[{"name":"trade-service","env":[{"name":"DB_POOL_SIZE","value":"5"}]}]}}}}'

# 3. Run maintenance operations
kubectl exec -it postgres-pod -- psql -U trade_user -d globeco_trade -c "REINDEX DATABASE globeco_trade;"
kubectl exec -it postgres-pod -- psql -U trade_user -d globeco_trade -c "VACUUM ANALYZE;"

# 4. Restore normal pool size
kubectl patch deployment trade-service --type='merge' -p='{"spec":{"template":{"spec":{"containers":[{"name":"trade-service","env":[{"name":"DB_POOL_SIZE","value":"20"}]}]}}}}'
```

#### Application Updates
```bash
# 1. Prepare new version
docker build -t globeco/trade-service:v2.1.0 .
docker push globeco/trade-service:v2.1.0

# 2. Update deployment with rolling update
kubectl set image deployment/trade-service trade-service=globeco/trade-service:v2.1.0

# 3. Monitor rollout
kubectl rollout status deployment/trade-service

# 4. Verify health
kubectl exec -it deployment/trade-service -- curl localhost:8080/actuator/health

# 5. Run smoke tests
./scripts/smoke-test.sh
```

### Cache Maintenance

#### Cache Warming
```bash
# Warm security cache with frequent tickers
curl -X POST "http://trade-service:8080/actuator/caches/security/warmup" \
  -H "Content-Type: application/json" \
  -d '["AAPL","MSFT","GOOGL","AMZN","TSLA"]'

# Warm portfolio cache with active portfolios
curl -X POST "http://trade-service:8080/actuator/caches/portfolio/warmup" \
  -H "Content-Type: application/json" \
  -d '["Growth Fund","Income Fund","Balanced Fund"]'
```

#### Cache Eviction
```bash
# Clear specific cache entries
curl -X DELETE "http://trade-service:8080/actuator/caches/security/AAPL"

# Clear entire cache
curl -X DELETE "http://trade-service:8080/actuator/caches/security"
```

## Security Procedures

### Access Management

#### Application Access
- Service accounts should use least privilege principle
- API keys should be rotated every 90 days
- Database credentials should be stored in Kubernetes secrets

#### Audit Procedures
```bash
# Review access logs
kubectl logs -l app=trade-service | grep -E "(POST|PUT|DELETE)" | tail -100

# Check failed authentication attempts
kubectl logs -l app=trade-service | grep -i "unauthorized\|forbidden" | tail -50

# Review database access
kubectl exec -it postgres-pod -- psql -U trade_user -d globeco_trade -c "
SELECT usename, datname, client_addr, application_name, state
FROM pg_stat_activity
WHERE state = 'active';"
```

### Security Incident Response

#### Suspected Breach
1. **Immediate Actions:**
   - Isolate affected systems
   - Preserve logs and evidence
   - Contact security team

2. **Investigation:**
   - Review access logs
   - Check for data exfiltration
   - Analyze attack vectors

3. **Recovery:**
   - Patch vulnerabilities
   - Reset credentials
   - Update security controls

### Data Protection

#### Backup Procedures
```bash
# Daily database backup
kubectl create job backup-$(date +%Y%m%d) --from=cronjob/postgres-backup

# Verify backup integrity
kubectl exec backup-pod -- pg_restore --list backup.sql | head -20

# Archive logs
kubectl exec -it trade-service-pod -- tar -czf logs-$(date +%Y%m%d).tar.gz /var/log/application/
```

#### Data Retention
- Application logs: 30 days
- Audit logs: 90 days
- Database backups: 1 year
- Metrics data: 6 months

## Performance Optimization

### Scaling Procedures

#### Horizontal Scaling
```bash
# Scale based on CPU usage
kubectl autoscale deployment trade-service --cpu-percent=70 --min=3 --max=20

# Manual scaling
kubectl scale deployment trade-service --replicas=10

# Verify scaling
kubectl get hpa trade-service
```

#### Vertical Scaling
```bash
# Increase memory limits
kubectl patch deployment trade-service -p '{"spec":{"template":{"spec":{"containers":[{"name":"trade-service","resources":{"limits":{"memory":"8Gi"},"requests":{"memory":"4Gi"}}}]}}}}'

# Increase CPU limits
kubectl patch deployment trade-service -p '{"spec":{"template":{"spec":{"containers":[{"name":"trade-service","resources":{"limits":{"cpu":"4"},"requests":{"cpu":"2"}}}]}}}}'
```

### Performance Tuning

#### JVM Tuning
```bash
# Update JVM options
kubectl patch deployment trade-service -p '{"spec":{"template":{"spec":{"containers":[{"name":"trade-service","env":[{"name":"JAVA_OPTS","value":"-Xms4g -Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"}]}]}}}}'
```

#### Database Tuning
```bash
# Add indexes for new query patterns
kubectl exec -it postgres-pod -- psql -U trade_user -d globeco_trade -c "
CREATE INDEX CONCURRENTLY idx_custom_filter 
ON trade_orders (portfolio_id, order_type, submitted) 
WHERE created_at > NOW() - INTERVAL '30 days';"

# Update statistics
kubectl exec -it postgres-pod -- psql -U trade_user -d globeco_trade -c "ANALYZE trade_orders;"
```

## Contact Information

### Escalation Contacts

| Role | Primary | Secondary | Phone | Email |
|------|---------|-----------|-------|-------|
| On-Call Engineer | John Doe | Jane Smith | +1-555-0123 | oncall@globeco.com |
| Team Lead | Alice Johnson | Bob Wilson | +1-555-0124 | teamlead@globeco.com |
| Service Owner | Carol Brown | David Davis | +1-555-0125 | serviceowner@globeco.com |
| DBA Team | - | - | +1-555-0126 | dba@globeco.com |
| Security Team | - | - | +1-555-0127 | security@globeco.com |

### External Dependencies

| Service | Contact | SLA | Support |
|---------|---------|-----|---------|
| Security Service | security-team@globeco.com | 99.9% | 24/7 |
| Portfolio Service | portfolio-team@globeco.com | 99.9% | Business Hours |
| Infrastructure | platform-team@globeco.com | 99.95% | 24/7 |

## Documentation References

- [Configuration Guide](./configuration-guide.md)
- [Performance Tuning Guide](./performance-tuning-guide.md)
- [Migration Guide](./v1-to-v2-migration-guide.md)
- [API Documentation](./src/main/resources/api-docs/openapi-v2.yaml)

## Appendix

### Useful Commands Reference

```bash
# Health Checks
curl http://trade-service:8080/actuator/health
curl http://trade-service:8080/actuator/health/readiness
curl http://trade-service:8080/actuator/health/liveness

# Metrics
curl http://trade-service:8080/actuator/metrics
curl http://trade-service:8080/actuator/prometheus
curl http://trade-service:8080/actuator/caches

# Application Info
curl http://trade-service:8080/actuator/info
curl http://trade-service:8080/actuator/env

# Diagnostics
curl http://trade-service:8080/actuator/threaddump
curl http://trade-service:8080/actuator/heapdump

# Kubernetes
kubectl get pods -l app=trade-service
kubectl logs -f deployment/trade-service
kubectl describe pod trade-service-xxx
kubectl exec -it trade-service-xxx -- bash
```

### Log Patterns

#### Common Error Patterns
```
# External service timeouts
"Read timed out executing GET http://security-service:8000/api/v2/securities"

# Database connection issues
"Connection is not available, request timed out after"

# Cache eviction warnings
"Cache eviction rate exceeding threshold"

# Circuit breaker activation
"CircuitBreaker 'securityService' is OPEN"
```

#### Important Success Patterns
```
# Successful startup
"Started GlobecoTradeServiceApplication in"

# Cache warming completed
"Cache warming completed for security cache"

# External service recovery
"CircuitBreaker 'securityService' is CLOSED"
``` 