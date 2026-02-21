package com.hsn.nmacircuitbreaker.breaker;

import com.hsn.nmacircuitbreaker.enums.State;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class CircuitBreaker {
    private final ScheduledExecutorService scheduler;
    private final double failureRateThreshold;
    private final int halfOpenTrialLimit;
    private final Duration openCoolDown;
    private final Deque<Boolean> slidingWindow;
    private final int slidingWindowSize;
    private final int minimumNumberOfCalls;


    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger halfOpenTrials = new AtomicInteger(0);
    private volatile State state = State.CLOSED;
    private int failureCountInWindow = 0;

    public CircuitBreaker(ScheduledExecutorService scheduler, double failureRateThreshold, int halfOpenTrialLimit
            , Duration openCoolDown, int slidingWindowSize, int minimumNumberOfCalls) {
        this.scheduler = scheduler;
        this.failureRateThreshold = failureRateThreshold;
        this.halfOpenTrialLimit = halfOpenTrialLimit;
        this.openCoolDown = openCoolDown;
        this.slidingWindowSize = slidingWindowSize;
        this.slidingWindow = new ArrayDeque<>(slidingWindowSize);
        this.minimumNumberOfCalls = minimumNumberOfCalls;
    }


    public <T> T execute(Supplier<T> action) {
        State current;
        synchronized (this) {
            current = state;
            if (current == State.OPEN) {
                throw new IllegalStateException("Circuit breaker is  OPEN, call Rejected..");

            }

            if (current == State.HALF_OPEN) {
                int trials = halfOpenTrials.incrementAndGet();
                if (trials > halfOpenTrialLimit) {
                    halfOpenTrials.decrementAndGet();
                    throw new IllegalStateException("Circuit breaker is HALF_OPEN. Trials limit exceeded..");
                }

            }
        }

        try {
            T result = action.get();

            onSuccess();
            return result;
        } catch (RuntimeException | Error e) {
            onFailure(e);
            throw e;
        } catch (Throwable t) {
            onFailure(t);
            throw new RuntimeException(t);
        }


    }

    private void onSuccess() {
        synchronized (this) {
            recordCallResult(true);
            if (state == State.HALF_OPEN) {
                transitionToClosed();
            }
        }
    }


    private void onFailure(Throwable t) {
        /**
         * for example, count server side failures,etc.
         * this is a placeholder for domain specific checks
         * if the failure is not relevant to that , then exit
         */
        boolean breakerRelevant = true;

        if (!breakerRelevant) {
            return;
        }

        synchronized (this) {
            recordCallResult(false);
            int total = slidingWindow.size();
            if (total >= minimumNumberOfCalls) {
                double failureRate = (failureCountInWindow * 100.0) / total;
                if (failureRate >= failureRateThreshold) {
                    transitionToOpen();
                }
            } else if (state == State.HALF_OPEN) {
                transitionToOpen();
            }
        }
    }


    private void transitionToHalfOpen() {
        synchronized (this) {
            state = State.HALF_OPEN;
            halfOpenTrials.set(0);
        }
    }

    private void transitionToOpen() {
        state = State.OPEN;
        failureCount.set(0);
        halfOpenTrials.set(0);
        scheduleHalfOPen();
    }

    private void transitionToClosed() {
        state = State.CLOSED;
        failureCount.set(0);
        halfOpenTrials.set(0);
    }

    private void scheduleHalfOPen() {
        scheduler.schedule(this::transitionToHalfOpen,
                openCoolDown.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void recordCallResult(boolean success) {
        synchronized (this) {
            if (slidingWindow.size() == slidingWindowSize) {
                boolean oldest = slidingWindow.removeFirst();
                if (!oldest) {
                    failureCountInWindow--;
                }
            }
            slidingWindow.addLast(success);
            if (!success) failureCountInWindow++;
        }
    }

}
