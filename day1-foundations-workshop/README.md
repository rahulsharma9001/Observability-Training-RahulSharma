# Day 1 Foundations Workshop: Logs and Metrics

## 1. Assignment Objective
Build an observable Spring Boot REST API that emits realistic telemetry under both normal and failure conditions, and visualize logs and metrics using a local observability stack (Prometheus, Loki, Grafana).

This implementation provides:
- A simulated Inventory API endpoint: `GET /api/items/{itemId}`
- Random delay and random failure injection for realistic traffic behavior
- Structured JSON logging with MDC context
- Prometheus metrics exposure using Spring Actuator
- A custom dimensional counter with dynamic `status` tags (`success`, `error`)
- Grafana dashboards and Loki Explore queries
- Integration tests validating metric behavior in success and error scenarios

---

## 2. Project Structure

```text
day1-foundations-workshop/
├── docker-compose.yml
├── observability/
│   ├── grafana/provisioning/...
│   ├── loki/loki-config.yml
│   ├── prometheus/prometheus.yml
│   └── promtail/promtail-config.yml
├── src/main/java/com/nashtech/observability/
│   ├── config/RequestMdcFilter.java
│   ├── controller/InventoryController.java
│   ├── exception/GlobalExceptionHandler.java
│   ├── metrics/RequestMetrics.java
│   └── service/InventoryServiceImpl.java
├── src/main/resources/
│   ├── application.properties
│   └── logback-spring.xml
├── src/test/java/com/nashtech/observability/controller/
│   └── InventoryControllerIntegrationTest.java
└── docs/screenshots/
```

---

## 3. Technology Stack
- Java 17
- Spring Boot 3.4.4
- Spring Web + Spring Actuator
- Micrometer + `micrometer-registry-prometheus`
- Logback + `logstash-logback-encoder`
- Prometheus + Loki + Promtail + Grafana (Docker Compose)
- JUnit 5 + MockMvc + Spring Boot Test

---

## 4. Functional Behavior Implemented

### API Endpoint
- `GET /api/items/{itemId}`
- Returns item payload with:
  - `itemId`
  - `itemName`
  - `availableQuantity`
  - `timestamp`

### Realistic Simulation
- Random processing delay using configurable min/max milliseconds:
  - `app.simulation.min-delay-ms`
  - `app.simulation.max-delay-ms`
- Random error generation using configurable error rate:
  - `app.simulation.error-rate`
- Failure scenario returns `HTTP 500` with JSON error body and `correlationId`

---

## 5. Observability Implementation

### 5.1 Structured Logging (JSON)
- Configured in `src/main/resources/logback-spring.xml`
- Logs are written to:
  - Console (JSON)
  - File: `logs/app.log` (JSON)
- Log levels covered in endpoint flow:
  - `INFO`: request received, request completed
  - `DEBUG`: simulation and successful processing details
  - `ERROR`: simulated failure with stack trace

### 5.2 MDC Usage
MDC keys added per request:
- `correlationId`
- `httpMethod`
- `requestPath`
- `itemId`

`RequestMdcFilter` handles correlation ID generation/propagation via `X-Correlation-Id` header and clears MDC safely after request completion.

### 5.3 Actuator & Prometheus Metrics Exposure
- Added dependency:
  - `io.micrometer:micrometer-registry-prometheus`
- Actuator endpoint enabled:
  - `GET /actuator/prometheus`
- Configuration in `src/main/resources/application.properties`:
  - `management.endpoints.web.exposure.include=health,info,metrics,prometheus`

### 5.4 Custom Dimensional Metric
Custom counter:
- Metric name: `inventory_requests_total`
- Dynamic tag:
  - `status="success"` for successful request completion
  - `status="error"` for simulated failed requests

Metric is incremented inside controller logic for both success and failure paths.

---

## 6. Local Observability Stack

`docker-compose.yml` provisions:
- Prometheus (`localhost:9090`)
- Loki (`localhost:3100`)
- Promtail (log shipper for `logs/app.log`)
- Grafana (`localhost:3000`, default `admin/admin`)

Grafana provisioning includes:
- Prometheus datasource
- Loki datasource
- Preloaded dashboard: `Day1 Observability Dashboard`

---

## 7. Run Instructions (Step-by-Step)

From project root:

```bash
cd /home/nashtech/Desktop/Observability-Pi-Shaped-Training/day1-foundations-workshop
```

### Step 1: Run tests

```bash
mvn clean test
```

If local Maven cache causes issues:

```bash
mvn -Dmaven.repo.local=/tmp/.m2 clean test
```

### Step 2: Start Spring Boot app

```bash
mvn spring-boot:run
```

### Step 3: Start observability stack (new terminal)

```bash
docker compose up -d
docker compose ps
```

### Step 4: Generate sample traffic (new terminal)

```bash
for i in $(seq 1 50); do curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/items/$i; done
```

You should see a mix of `200` and `500` status codes.

---

## 8. Verification Commands

### API and Actuator

```bash
curl -i http://localhost:8080/api/items/1001
curl -i http://localhost:8080/actuator/health
curl -s http://localhost:8080/actuator/prometheus | grep inventory_requests_total
```

### Structured JSON logs in terminal

```bash
tail -f logs/app.log
```

### Grafana / Prometheus / Loki checks
- Grafana: `http://localhost:3000`
- Prometheus UI: `http://localhost:9090`
- Loki endpoint: `http://localhost:3100`

Prometheus query:

```promql
sum by (status) (inventory_requests_total)
```

Loki Explore query:

```logql
{job="day1-foundations-workshop"} | json
```

Loki error-only filter:

```logql
{job="day1-foundations-workshop", level="ERROR"} | json
```

---

## 9. Testing Coverage

Integration tests in:

`src/test/java/com/nashtech/observability/controller/InventoryControllerIntegrationTest.java`

Test cases:
1. Success path increments `inventory_requests_total{status="success"}`
2. Error path increments `inventory_requests_total{status="error"}`

These tests validate metric increments independent of API outcome.

---

## 10. Screenshots (Visual Proof)

### A) Grafana Dashboard - Custom Counter with Dynamic Tags
Expected panel should show `status=success` and `status=error`.

![Grafana Dashboard - Custom Counter](Screenshots/Grafana-Custom-Dashboard.png)

Grafana-Custom-Dashboard.png

### B) Grafana Explore - Loki JSON Structured Logs
Expected query in Explore:
`{job="day1-foundations-workshop"} | json`

![Grafana Explore - Loki JSON Logs](Screenshots/LOKI-JSON-LOGS-1.png)

![Grafana Explore - Loki JSON Logs](Screenshots/LOKI-JSON-LOGS-2.png)


### C) Terminal Output - Raw JSON Structured Logs
Capture output from:
`tail -f logs/app.log`

![Terminal - Raw JSON Logs](Screenshots/Terminal-JSON-Logs.png)

---
