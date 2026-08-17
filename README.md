<p align="center">
  <a href="https://xyo.financial" target="_blank" rel="noopener noreferrer">
    <img alt="XYO Financial Java Mascot" width="380" src="https://raw.githubusercontent.com/xyo-financial/sdk-java/main/docs/mascot.png" />
  </a>
</p>

<h1 align="center">XYO Financial SDK for Java</h1>

<p align="center">
  <a href="https://github.com/xyo-financial/sdk-java/actions/workflows/ci.yml"><img src="https://github.com/xyo-financial/sdk-java/actions/workflows/ci.yml/badge.svg?branch=main" alt="CI Build" /></a>
  <a href="https://github.com/xyo-financial/sdk-java/actions/workflows/release.yml"><img src="https://github.com/xyo-financial/sdk-java/actions/workflows/release.yml/badge.svg" alt="Release Pipeline" /></a>
  <img src="https://img.shields.io/badge/Java-17%2B-blue" alt="Java 17+" />
  <img src="https://img.shields.io/badge/Maven_Central-com.xyo%3Axyo--sdk-informational" alt="Maven Central" />
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="License" /></a>
</p>

<p align="center">
  <strong>The official Java SDK for <a href="https://xyo.financial">XYO Financial</a>.</strong><br>
  Seamlessly enrich raw financial transactions into clean merchant profiles, intelligent business categorizations, high-res logos, and geolocated address metadata using AI-powered enrichment pipelines.
</p>

---

## 📖 Summary

The **XYO Financial Java SDK** delivers a high-performance, strictly typed, thread-safe client library for integrating XYO's transaction enrichment engine into enterprise Java ecosystems. Engineered for Tier-1 financial institutions, neobanks, payment processors, and fintech platforms, this SDK transforms raw, cryptic payment counterparty strings into structured, categorized, and geocoded merchant intelligence.

Maintained by [Syniol Limited](https://syniol.com) as the official Java distribution for [XYO.Financial](https://xyo.financial), the SDK supports both low-latency synchronous transaction processing paths and high-throughput asynchronous bulk batches.

---

## 🏗 Architectural Principles

1. **Modern Java 17+ Baseline**: Leverages modern Java language features, immutable record-style builders, JSpecify nullability annotations (`@Nullable`), and native `java.net.http.HttpClient` transport.
2. **Thread Safety & Immutability**: All client and model instances are defensive-copied and thread-safe. A single `XyoClient` instance can be safely injected across multiple concurrent application threads.
3. **Resilience & Bounded Latency**: Granular connection timeout, request timeout, and maximum response payload bounds prevent runaway latency and memory exhaustion in mission-critical payment pipelines.
4. **RFC 7807 Problem Details Compliance**: Rich, structured error handling categorizes operational errors (`HTTP`, `TRANSPORT`, `VALIDATION`, `PARSING`) and surfaces RFC 7807 compliant problem details for automated Dead-Letter Queue (DLQ) routing and remediation.
5. **Security & Zero PII Footprint**: Enforces HTTPS encryption by default (preventing accidental insecure downgrade attacks). Requires only counterparty description and ISO country code—never PAN, CVV, or regulated account credentials.

---

## ⚙️ System Requirements

* **JDK**: Java 17 LTS or newer (tested on Java 17, 21, and 22).
* **Build System**: Maven 3.8+ or Gradle 7.0+.
* **Credentials**: A valid API Key from the [XYO Dashboard](https://xyo.financial/dashboard).

---

## 📦 Installation

The SDK is published to Maven Central. Add the dependency to your build configuration:

### Maven (`pom.xml`)

```xml
<dependency>
    <groupId>com.xyo</groupId>
    <artifactId>xyo-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

*(If utilizing the internal financial suite distribution coordinates):*

```xml
<dependency>
    <groupId>com.xyo.financial</groupId>
    <artifactId>xyo-sdk</artifactId>
    <version>1.0.2</version>
</dependency>
```

### Gradle (`build.gradle` - Groovy DSL)

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.xyo:xyo-sdk:1.0.0'
}
```

### Gradle (`build.gradle.kts` - Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.xyo:xyo-sdk:1.0.0")
}
```

---

## 🚀 Quickstart Guide

### 1. Client Configuration & Initialization

Initialize `XyoClient` using the immutable `ClientConfig.Builder`. In enterprise environments, manage `XyoClient` as a singleton managed bean (e.g., via Spring `@Bean`, Quarkus `@ApplicationScoped`, or Guice `@Provides`).

```java
package com.example.billing;

import com.xyo.financial.ClientConfig;
import com.xyo.financial.XyoClient;
import java.net.http.HttpClient;
import java.time.Duration;

public class XyoClientProvider {

    public static XyoClient createClient() {
        ClientConfig config = new ClientConfig.Builder(System.getenv("XYO_API_KEY"))
                .apiBaseUrl(ClientConfig.DEFAULT_API_BASE_URL) // https://api.xyo.financial
                .connectTimeoutMs(5000)                        // 5s connection timeout
                .requestTimeoutMs(15000)                       // 15s request read timeout
                .maxResponseBytes(1024 * 1024)                 // 1MB response size guard
                .allowInsecureHttp(false)                      // Strict TLS enforcement
                .httpClient(HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build())
                .build();

        return new XyoClient(config);
    }
}
```

---

### 2. Enrich Single Payment Transaction (`enrichTransaction`)

Execute real-time, synchronous transaction enrichment on payment authorization hooks or statement view generation:

```java
package com.example.billing;

import com.xyo.financial.EnrichmentRequest;
import com.xyo.financial.EnrichmentResponse;
import com.xyo.financial.XyoClient;
import com.xyo.financial.XyoException;

public class TransactionEnrichmentService {

    private final XyoClient xyoClient;

    public TransactionEnrichmentService(XyoClient xyoClient) {
        this.xyoClient = xyoClient;
    }

    public void processPayment(String rawDescription, String isoCountryCode) {
        try {
            EnrichmentRequest request = new EnrichmentRequest(rawDescription, isoCountryCode);
            // Alternatively using the Builder:
            // EnrichmentRequest request = new EnrichmentRequest.Builder()
            //         .content(rawDescription)
            //         .countryCode(isoCountryCode)
            //         .build();

            EnrichmentResponse response = xyoClient.enrichTransaction(request);

            System.out.println("Merchant Identified: " + response.getMerchant());
            System.out.println("Clean Description:   " + response.getDescription());
            System.out.println("Categories:          " + String.join(", ", response.getCategories()));
            System.out.println("Logo URL:            " + response.getLogo());
            System.out.println("Location:            " + (response.getLocation() != null ? response.getLocation() : "N/A"));
            System.out.println("Address:             " + (response.getAddress() != null ? response.getAddress() : "N/A"));

        } catch (XyoException ex) {
            System.err.printf("Enrichment failed [%s]: %s (HTTP %d)%n",
                    ex.getCategory(), ex.getMessage(), ex.getHttpStatusCode());
            throw ex;
        }
    }
}
```

---

### 3. Bulk Transaction Enrichment (`enrichTransactionCollection`)

For high-volume ETL pipelines, nightly reconciliations, or large statement migrations, submit batches asynchronously:

```java
package com.example.billing;

import com.xyo.financial.EnrichTransactionCollectionResponse;
import com.xyo.financial.EnrichmentRequest;
import com.xyo.financial.XyoClient;
import java.util.List;

public class BatchEnrichmentService {

    private final XyoClient xyoClient;

    public BatchEnrichmentService(XyoClient xyoClient) {
        this.xyoClient = xyoClient;
    }

    public EnrichTransactionCollectionResponse submitBatch(List<EnrichmentRequest> transactions) {
        // Submit collection for asynchronous processing
        // Optional x-api-user header can also be passed via overload: enrichTransactionCollection(transactions, "tenant-user-123")
        EnrichTransactionCollectionResponse response = xyoClient.enrichTransactionCollection(transactions);

        System.out.println("Batch Queued Successfully!");
        System.out.println("Batch ID:    " + response.getId());
        System.out.println("Status Link: " + response.getLink());

        return response;
    }
}
```

---

### 4. Check Collection Status (`enrichTransactionCollectionStatus`)

Poll or track the progress of an asynchronous bulk enrichment task:

```java
package com.example.billing;

import com.xyo.financial.EnrichmentCollectionStatus;
import com.xyo.financial.XyoClient;

public class BatchStatusMonitor {

    private final XyoClient xyoClient;

    public BatchStatusMonitor(XyoClient xyoClient) {
        this.xyoClient = xyoClient;
    }

    public EnrichmentCollectionStatus checkBatchStatus(String batchId) throws InterruptedException {
        EnrichmentCollectionStatus status = xyoClient.enrichTransactionCollectionStatus(batchId);

        while (status == EnrichmentCollectionStatus.PENDING) {
            System.out.println("Batch " + batchId + " is still processing. Waiting 3 seconds...");
            Thread.sleep(3000);
            status = xyoClient.enrichTransactionCollectionStatus(batchId);
        }

        switch (status) {
            case READY -> System.out.println("Batch " + batchId + " is READY for result retrieval.");
            case FAILED -> System.err.println("Batch " + batchId + " processing FAILED.");
            default -> throw new IllegalStateException("Unexpected status: " + status);
        }

        return status;
    }
}
```

---

### 5. Download Bulk Enrichment Results (`downloadEnrichmentCollection`)

Once a bulk enrichment job has reached `READY` status, download and decompress the `.tar.gz` archive of enriched results:

```java
package com.example.billing;

import com.xyo.financial.EnrichmentCollectionStatus;
import com.xyo.financial.EnrichmentResponse;
import com.xyo.financial.XyoClient;
import com.xyo.financial.XyoException;
import java.util.List;

public class BatchResultDownloader {

    private final XyoClient xyoClient;

    public BatchResultDownloader(XyoClient xyoClient) {
        this.xyoClient = xyoClient;
    }

    public List<EnrichmentResponse> downloadResults(String downloadUrl) {
        try {
            // Downloads .tar.gz archive, decompresses gzip stream, and parses JSON records
            List<EnrichmentResponse> results = xyoClient.downloadEnrichmentCollection(downloadUrl);

            System.out.printf("Downloaded %d enriched transaction records.%n", results.size());
            for (EnrichmentResponse item : results) {
                System.out.printf("  - %s: %s [%s]%n",
                        item.getMerchant(),
                        item.getDescription(),
                        String.join(", ", item.getCategories()));
            }

            return results;
        } catch (XyoException ex) {
            System.err.printf("Failed to download results [%s]: %s (HTTP %d)%n",
                    ex.getCategory(), ex.getMessage(), ex.getHttpStatusCode());
            throw ex;
        }
    }
}
```

---

## 🚀 Framework & Architecture Integration

The `XyoClient` is engineered for modern enterprise microservices, cloud-native deployments, and serverless runtimes. Designed to be strictly thread-safe and immutable, a single `XyoClient` instance should be registered as a singleton bean in your dependency injection container and shared across concurrent execution threads.

### Spring Boot 3.x / Jakarta EE

Define a `@Configuration` class to expose `XyoClient` as a Spring-managed bean. Specifying `@Bean(destroyMethod = "close")` ensures proper lifecycle teardown when the `ApplicationContext` is closed:

```java
package com.example.config;

import com.xyo.financial.ClientConfig;
import com.xyo.financial.XyoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class XyoConfig {

    @Bean(destroyMethod = "close")
    public XyoClient xyoClient(@Value("${xyo.api.key}") String apiKey) {
        ClientConfig config = new ClientConfig.Builder(apiKey)
                .connectTimeoutMs(2000)
                .requestTimeoutMs(2000)
                .build();

        return new XyoClient(config);
    }
}
```

Inject and consume anywhere across your Spring services or controllers:

```java
package com.example.billing;

import com.xyo.financial.EnrichmentRequest;
import com.xyo.financial.EnrichmentResponse;
import com.xyo.financial.XyoClient;
import org.springframework.stereotype.Service;

@Service
public class PaymentProcessingService {

    private final XyoClient xyoClient;

    public PaymentProcessingService(XyoClient xyoClient) {
        this.xyoClient = xyoClient;
    }

    public EnrichmentResponse process(String rawMerchant, String countryCode) {
        return xyoClient.enrichTransaction(new EnrichmentRequest(rawMerchant, countryCode));
    }
}
```

---

### Quarkus & Jakarta CDI

For Quarkus applications, declare `XyoClient` as an `@ApplicationScoped` CDI producer:

```java
package com.example.config;

import com.xyo.financial.ClientConfig;
import com.xyo.financial.XyoClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class XyoClientProducer {

    @Produces
    @ApplicationScoped
    public XyoClient produceXyoClient(@ConfigProperty(name = "xyo.api.key") String apiKey) {
        ClientConfig config = new ClientConfig.Builder(apiKey)
                .connectTimeoutMs(2000)
                .requestTimeoutMs(2000)
                .build();

        return new XyoClient(config);
    }

    public void close(@Disposes XyoClient client) {
        client.close();
    }
}
```

---

### Micronaut Framework

For Micronaut microservices, register a singleton factory bean:

```java
package com.example.config;

import com.xyo.financial.ClientConfig;
import com.xyo.financial.XyoClient;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

@Factory
public class XyoClientFactory {

    @Singleton
    public XyoClient xyoClient(@Value("${xyo.api.key}") String apiKey) {
        ClientConfig config = new ClientConfig.Builder(apiKey)
                .connectTimeoutMs(2000)
                .requestTimeoutMs(2000)
                .build();

        return new XyoClient(config);
    }
}
```

---

### GraalVM Native Image & AOT Compatibility

The XYO Java SDK is designed for **Ahead-of-Time (AOT)** compilation with **GraalVM Native Image**, **Quarkus Native**, and **Spring Boot Native AOT**:

* **⚡ Sub-8ms Cold Starts**: Instant startup time for serverless functions (AWS Lambda, Google Cloud Run, Azure Functions) and scale-to-zero workloads.
* **🛡️ Zero Runtime Reflection Proxies**: Built on JDK 17+ native `java.net.http.HttpClient` transport and Jackson serialization with no dynamic bytecode generation or CGLIB proxies.
* **💾 Ultra-Low Memory (<25MB RSS)**: Minimal resident memory consumption on containerized platforms (Kubernetes, Red Hat OpenShift, AWS ECS/Fargate), maximizing vertical pod density.
* **🔒 Native TLS Transport**: Built-in TLS 1.3 / 1.2 negotiation without requiring external C/JNI crypto binaries.

---

## 🛡️ Robust RFC 7807 Error Handling

The XYO API adheres to the **RFC 7807 (Problem Details for HTTP APIs)** specification. Non-2xx HTTP responses return structured problem detail documents (`application/problem+json` or `application/json`).

The Java SDK captures both low-level transport/OpenAPI exceptions (`ApiException`) and normalizes all failures into strongly typed `XyoException` runtime exceptions with discrete `ErrorCategory` classifications.

### Error Taxonomy

| Category (`ErrorCategory`) | Description | Typical Cause | Enterprise Mitigation |
|:---|:---|:---|:---|
| `VALIDATION` | Client-side validation failure | Blank content, invalid country code, null config | Validate upstream transaction attributes before sending. |
| `HTTP` | Non-2xx response from XYO API | 400 Bad Request, 401 Unauthorized, 429 Rate Limit | Parse RFC 7807 JSON body (`ex.getResponseBody()`) for field-level error details. |
| `TRANSPORT` | Network / I/O transport fault | Connect timeout, TCP reset, DNS resolution failure | Trigger exponential backoff retry with jitter. |
| `PARSING` | Response deserialization failure | Malformed payload or unparseable schema | Inspect raw payload; alert Syniol API support team. |

---

### RFC 7807 Parsing Pattern

```java
package com.example.billing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyo.client.ApiException;
import com.xyo.financial.ErrorCategory;
import com.xyo.financial.XyoException;

public class EnterpriseErrorHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * RFC 7807 Problem Details representation.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProblemDetails(
            String type,
            String title,
            Integer status,
            String detail,
            String instance
    ) {}

    public static void handleException(XyoException ex) {
        if (ex.getCategory() == ErrorCategory.HTTP) {
            int httpStatus = ex.getHttpStatusCode();
            String rawBody = ex.getResponseBody();

            System.err.printf("HTTP Error %d encountered.%n", httpStatus);

            if (rawBody != null && !rawBody.isBlank()) {
                try {
                    ProblemDetails problem = OBJECT_MAPPER.readValue(rawBody, ProblemDetails.class);
                    System.err.printf("RFC 7807 Detail -> Title: '%s', Detail: '%s', Type: '%s'%n",
                            problem.title(), problem.detail(), problem.type());
                } catch (Exception parseEx) {
                    System.err.println("Raw Response Body: " + rawBody);
                }
            }

            switch (httpStatus) {
                case 400 -> System.err.println("Routing to Dead-Letter Queue (DLQ) for manual inspection.");
                case 401, 403 -> System.err.println("CRITICAL: Invalid API Key. Triggering SecOps key rotation alert.");
                case 429 -> System.err.println("Rate limit reached. Applying exponential backoff with jitter.");
                case 500, 502, 503, 504 -> System.err.println("Upstream service degradation. Engaging circuit-breaker.");
                default -> System.err.println("Unhandled HTTP status code.");
            }
        } else if (ex.getCategory() == ErrorCategory.TRANSPORT) {
            System.err.println("Transport failure: " + ex.getMessage() + " (Cause: " + ex.getCause() + ")");
        } else if (ex.getCategory() == ErrorCategory.VALIDATION) {
            System.err.println("Client validation failed: " + ex.getMessage());
        }
    }
}
```

---

## 🔒 Security & Operational Best Practices

1. **Credential Management**: Store your API key securely using a Secret Manager (AWS Secrets Manager, HashiCorp Vault, Azure Key Vault, or Kubernetes Secrets). Do not hardcode API keys in source control.
2. **Connection Pooling**: When passing a custom `HttpClient`, configure appropriate connection pool sizing and HTTP/2 multiplexing for optimal throughput.
3. **Timeouts**: Always configure explicit connect and request timeouts. The default SDK timeout is 5 seconds for connection and 30 seconds for request completion.
4. **Data Minimization**: Submit only transaction descriptions (e.g., `"TFL TRAVEL CHARGE"`) and ISO country codes (e.g., `"GB"`). Never transmit cardholder names, PANs, CVVs, or bank account credentials.

---

## 🛠️ Building and Testing from Source

```bash
# Clone the repository
git clone https://github.com/xyo-financial/sdk-java.git
cd sdk-java

# Compile and execute the full test suite (31 unit & integration boundary tests)
mvn clean test

# Install to local Maven repository cache
mvn clean install
```

---

## 📄 License

This project is licensed under the **Apache License, Version 2.0** - see the [LICENSE](LICENSE) file for details.

Copyright &copy; 2026 [Syniol Limited](https://syniol.com). All rights reserved.