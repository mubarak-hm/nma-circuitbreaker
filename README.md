#  Custom Java Circuit Breaker

A lightweight circuit breaker implementation in Java with sliding-window failure rate tracking and scheduler-based recovery — designed to help  understand resilience patterns 

This implementation is inspired by production libraries like Resilience4j, but built from scratch so the internal mechanics are clear and customizable.

---

##  What It Does

This circuit breaker:

- Tracks recent call outcomes in a sliding window
- Calculates failure rate rather than just consecutive failures
- Opens the circuit when failure rate exceeds threshold
- Uses a scheduler to move from OPEN → HALF_OPEN
- Allows a limited number of trial calls in HALF_OPEN
- Closes the circuit again when recovery succeeds

---

##  When It Opens

The circuit enters **OPEN** when:

 The percentage of recent failures ≥ configured threshold  
AND  
 The number of calls in the window ≥ minimum number of calls

Once OPEN, calls are rejected immediately until the cooldown expires.

---

  HALF_OPEN Behavior

In HALF_OPEN:

- A limited number of calls are allowed (`halfOpenTrialLimit`)
- On success → circuit moves to **CLOSED**
- On failure → circuit moves back to **OPEN**

This helps test whether the dependency has recovered.

---

  Example Usage

```java
CircuitBreaker breaker = new CircuitBreaker(
    Executors.newSingleThreadScheduledExecutor(),
    50.0, // failure rate threshold (%)
    5,    // half-open trial limit
    Duration.ofSeconds(30),
    20,   // sliding window size
    10    // minimum number of calls before evaluating failure rate
);

String result = breaker.execute(() ->
    restTemplate.getForObject(url, String.class)
);
