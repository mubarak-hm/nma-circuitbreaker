package com.hsn.nmacircuitbreaker.service;

import com.hsn.nmacircuitbreaker.breaker.CircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ExternalApiService {

    private final CircuitBreaker circuitBreaker;
    private final RestTemplate restTemplate;

    public ExternalApiService(CircuitBreaker circuitBreaker, RestTemplate restTemplate) {
        this.circuitBreaker = circuitBreaker;
        this.restTemplate = restTemplate;
    }


    public String callExternal() {
        return circuitBreaker.execute(() ->
                restTemplate.getForObject("http://external/api", String.class)
        );

    }
}
