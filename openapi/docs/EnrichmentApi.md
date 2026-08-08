# EnrichmentApi

All URIs are relative to *https://api.xyo.financial*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**enrichTransaction**](EnrichmentApi.md#enrichTransaction) | **POST** /v1/ai/finance/enrichment/transaction | Transaction Enrichment |
| [**enrichTransactionWithHttpInfo**](EnrichmentApi.md#enrichTransactionWithHttpInfo) | **POST** /v1/ai/finance/enrichment/transaction | Transaction Enrichment |
| [**enrichTransactions**](EnrichmentApi.md#enrichTransactions) | **POST** /v1/ai/finance/enrichment/transactions | Transaction Enrichments |
| [**enrichTransactionsWithHttpInfo**](EnrichmentApi.md#enrichTransactionsWithHttpInfo) | **POST** /v1/ai/finance/enrichment/transactions | Transaction Enrichments |
| [**getEnrichmentStatus**](EnrichmentApi.md#getEnrichmentStatus) | **GET** /v1/ai/finance/enrichment/status/{id} | Transaction Enrichments Status |
| [**getEnrichmentStatusWithHttpInfo**](EnrichmentApi.md#getEnrichmentStatusWithHttpInfo) | **GET** /v1/ai/finance/enrichment/status/{id} | Transaction Enrichments Status |



## enrichTransaction

> EnrichmentResponse enrichTransaction(enrichmentRequest)

Transaction Enrichment

Enrich a single financial transaction synchronously.

### Example

```java
// Import classes:
import com.xyo.client.ApiClient;
import com.xyo.client.ApiException;
import com.xyo.client.Configuration;
import com.xyo.client.auth.*;
import com.xyo.client.models.*;
import com.xyo.api.EnrichmentApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://api.xyo.financial");
        
        // Configure HTTP bearer authorization: BearerAuth
        HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
        BearerAuth.setBearerToken("BEARER TOKEN");

        EnrichmentApi apiInstance = new EnrichmentApi(defaultClient);
        EnrichmentRequest enrichmentRequest = new EnrichmentRequest(); // EnrichmentRequest | 
        try {
            EnrichmentResponse result = apiInstance.enrichTransaction(enrichmentRequest);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling EnrichmentApi#enrichTransaction");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **enrichmentRequest** | [**EnrichmentRequest**](EnrichmentRequest.md)|  | [optional] |

### Return type

[**EnrichmentResponse**](EnrichmentResponse.md)


### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful response |  -  |
| **3XX** | Redirection |  -  |
| **4XX** | Client Error |  -  |

## enrichTransactionWithHttpInfo

> ApiResponse<EnrichmentResponse> enrichTransactionWithHttpInfo(enrichmentRequest)

Transaction Enrichment

Enrich a single financial transaction synchronously.

### Example

```java
// Import classes:
import com.xyo.client.ApiClient;
import com.xyo.client.ApiException;
import com.xyo.client.ApiResponse;
import com.xyo.client.Configuration;
import com.xyo.client.auth.*;
import com.xyo.client.models.*;
import com.xyo.api.EnrichmentApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://api.xyo.financial");
        
        // Configure HTTP bearer authorization: BearerAuth
        HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
        BearerAuth.setBearerToken("BEARER TOKEN");

        EnrichmentApi apiInstance = new EnrichmentApi(defaultClient);
        EnrichmentRequest enrichmentRequest = new EnrichmentRequest(); // EnrichmentRequest | 
        try {
            ApiResponse<EnrichmentResponse> response = apiInstance.enrichTransactionWithHttpInfo(enrichmentRequest);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling EnrichmentApi#enrichTransaction");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **enrichmentRequest** | [**EnrichmentRequest**](EnrichmentRequest.md)|  | [optional] |

### Return type

ApiResponse<[**EnrichmentResponse**](EnrichmentResponse.md)>


### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful response |  -  |
| **3XX** | Redirection |  -  |
| **4XX** | Client Error |  -  |


## enrichTransactions

> EnrichTransactionCollectionResponse enrichTransactions(xApiUser, enrichTransactionsRequestInner)

Transaction Enrichments

Enrich a collection of financial transactions asynchronously.

### Example

```java
// Import classes:
import com.xyo.client.ApiClient;
import com.xyo.client.ApiException;
import com.xyo.client.Configuration;
import com.xyo.client.auth.*;
import com.xyo.client.models.*;
import com.xyo.api.EnrichmentApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://api.xyo.financial");
        
        // Configure HTTP bearer authorization: BearerAuth
        HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
        BearerAuth.setBearerToken("BEARER TOKEN");

        EnrichmentApi apiInstance = new EnrichmentApi(defaultClient);
        Object xApiUser = syniol; // Object | 
        List<EnrichTransactionsRequestInner> enrichTransactionsRequestInner = Arrays.asList(); // List<EnrichTransactionsRequestInner> | 
        try {
            EnrichTransactionCollectionResponse result = apiInstance.enrichTransactions(xApiUser, enrichTransactionsRequestInner);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling EnrichmentApi#enrichTransactions");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **xApiUser** | [**Object**](.md)|  | [optional] |
| **enrichTransactionsRequestInner** | [**List&lt;EnrichTransactionsRequestInner&gt;**](EnrichTransactionsRequestInner.md)|  | [optional] |

### Return type

[**EnrichTransactionCollectionResponse**](EnrichTransactionCollectionResponse.md)


### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful response |  -  |
| **3XX** | Redirection |  -  |
| **4XX** | Client Error |  -  |

## enrichTransactionsWithHttpInfo

> ApiResponse<EnrichTransactionCollectionResponse> enrichTransactionsWithHttpInfo(xApiUser, enrichTransactionsRequestInner)

Transaction Enrichments

Enrich a collection of financial transactions asynchronously.

### Example

```java
// Import classes:
import com.xyo.client.ApiClient;
import com.xyo.client.ApiException;
import com.xyo.client.ApiResponse;
import com.xyo.client.Configuration;
import com.xyo.client.auth.*;
import com.xyo.client.models.*;
import com.xyo.api.EnrichmentApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://api.xyo.financial");
        
        // Configure HTTP bearer authorization: BearerAuth
        HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
        BearerAuth.setBearerToken("BEARER TOKEN");

        EnrichmentApi apiInstance = new EnrichmentApi(defaultClient);
        Object xApiUser = syniol; // Object | 
        List<EnrichTransactionsRequestInner> enrichTransactionsRequestInner = Arrays.asList(); // List<EnrichTransactionsRequestInner> | 
        try {
            ApiResponse<EnrichTransactionCollectionResponse> response = apiInstance.enrichTransactionsWithHttpInfo(xApiUser, enrichTransactionsRequestInner);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling EnrichmentApi#enrichTransactions");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **xApiUser** | [**Object**](.md)|  | [optional] |
| **enrichTransactionsRequestInner** | [**List&lt;EnrichTransactionsRequestInner&gt;**](EnrichTransactionsRequestInner.md)|  | [optional] |

### Return type

ApiResponse<[**EnrichTransactionCollectionResponse**](EnrichTransactionCollectionResponse.md)>


### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful response |  -  |
| **3XX** | Redirection |  -  |
| **4XX** | Client Error |  -  |


## getEnrichmentStatus

> EnrichmentCollectionStatusResponse getEnrichmentStatus(id, xApiUser)

Transaction Enrichments Status

Get the status of an asynchronous bulk enrichment job.

### Example

```java
// Import classes:
import com.xyo.client.ApiClient;
import com.xyo.client.ApiException;
import com.xyo.client.Configuration;
import com.xyo.client.auth.*;
import com.xyo.client.models.*;
import com.xyo.api.EnrichmentApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://api.xyo.financial");
        
        // Configure HTTP bearer authorization: BearerAuth
        HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
        BearerAuth.setBearerToken("BEARER TOKEN");

        EnrichmentApi apiInstance = new EnrichmentApi(defaultClient);
        String id = "id_example"; // String | 
        Object xApiUser = syniol; // Object | 
        try {
            EnrichmentCollectionStatusResponse result = apiInstance.getEnrichmentStatus(id, xApiUser);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling EnrichmentApi#getEnrichmentStatus");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **id** | **String**|  | |
| **xApiUser** | [**Object**](.md)|  | [optional] |

### Return type

[**EnrichmentCollectionStatusResponse**](EnrichmentCollectionStatusResponse.md)


### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful response |  -  |
| **3XX** | Redirection |  -  |
| **4XX** | Client Error |  -  |

## getEnrichmentStatusWithHttpInfo

> ApiResponse<EnrichmentCollectionStatusResponse> getEnrichmentStatusWithHttpInfo(id, xApiUser)

Transaction Enrichments Status

Get the status of an asynchronous bulk enrichment job.

### Example

```java
// Import classes:
import com.xyo.client.ApiClient;
import com.xyo.client.ApiException;
import com.xyo.client.ApiResponse;
import com.xyo.client.Configuration;
import com.xyo.client.auth.*;
import com.xyo.client.models.*;
import com.xyo.api.EnrichmentApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://api.xyo.financial");
        
        // Configure HTTP bearer authorization: BearerAuth
        HttpBearerAuth BearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("BearerAuth");
        BearerAuth.setBearerToken("BEARER TOKEN");

        EnrichmentApi apiInstance = new EnrichmentApi(defaultClient);
        String id = "id_example"; // String | 
        Object xApiUser = syniol; // Object | 
        try {
            ApiResponse<EnrichmentCollectionStatusResponse> response = apiInstance.getEnrichmentStatusWithHttpInfo(id, xApiUser);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling EnrichmentApi#getEnrichmentStatus");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **id** | **String**|  | |
| **xApiUser** | [**Object**](.md)|  | [optional] |

### Return type

ApiResponse<[**EnrichmentCollectionStatusResponse**](EnrichmentCollectionStatusResponse.md)>


### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful response |  -  |
| **3XX** | Redirection |  -  |
| **4XX** | Client Error |  -  |

