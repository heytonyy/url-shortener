package com.urlshortener.api.service;

import com.urlshortener.api.entity.GlobalCounter;
import com.urlshortener.api.entity.RangeAllocation;
import com.urlshortener.api.repository.GlobalCounterRepository;
import com.urlshortener.api.repository.RangeAllocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for generating unique short codes using distributed range allocation.
 * Each service instance maintains a local range of codes to avoid database lookups.
 * 
 * Algorithm:
 * 1. On startup, allocate a range from the global counter (e.g., 0-999)
 * 2. Generate codes by incrementing local counter and encoding to Base62
 * 3. When range is low, request a new range asynchronously
 * 4. Each instance has exclusive ownership of its ranges
 */
@Service
public class ShortCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(ShortCodeGenerator.class);

    private final GlobalCounterRepository globalCounterRepository;
    private final RangeAllocationRepository rangeAllocationRepository;

    @Value("${app.range.size:1000}")
    private Integer rangeSize;

    @Value("${app.range.threshold:100}")
    private Integer rangeThreshold;

    @Value("${app.shortcode.charset:0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ}")
    private String charset;

    private String serviceInstanceId;
    private AtomicLong currentCounter;
    private Long rangeEnd;
    private final Object rangeLock = new Object();

    // Constructor
    public ShortCodeGenerator(GlobalCounterRepository globalCounterRepository,
                              RangeAllocationRepository rangeAllocationRepository) {
        this.globalCounterRepository = globalCounterRepository;
        this.rangeAllocationRepository = rangeAllocationRepository;
    }

    /**
     * Initialize on startup - allocate initial range
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initialize() {
        try {
            // Generate unique service instance ID
            serviceInstanceId = InetAddress.getLocalHost().getHostName() + "-" + System.currentTimeMillis();
            log.info("Service instance ID: {}", serviceInstanceId);

            // Allocate initial range
            allocateNewRangeInternal();
            
            log.info("ShortCodeGenerator initialized with range [{}, {}]", 
                    currentCounter.get(), rangeEnd);
        } catch (Exception e) {
            log.error("Failed to initialize ShortCodeGenerator", e);
            throw new RuntimeException("Failed to initialize short code generator", e);
        }
    }

    /**
     * Generate a unique short code
     * 
     * @return A unique short code (Base62 encoded)
     */
    public String generateShortCode() {
        long code = getNextCode();
        return encodeBase62(code);
    }

    /**
     * Get the next available code from the current range.
     * Automatically allocates a new range when threshold is reached.
     * 
     * @return The next sequential number
     */
    private long getNextCode() {
        synchronized (rangeLock) {
            long code = currentCounter.getAndIncrement();
            
            // Check if we need a new range
            long remaining = rangeEnd - code;
            if (remaining < rangeThreshold) {
                log.warn("Range running low. Remaining codes: {}. Allocating new range...", remaining);
                // In production, this should be async to avoid blocking
                allocateNewRangeWithTransaction();
            }
            
            if (code > rangeEnd) {
                log.error("Code {} exceeded range end {}. This should not happen!", code, rangeEnd);
                throw new IllegalStateException("Exceeded allocated range");
            }
            
            return code;
        }
    }

    /**
     * Public transactional method for allocating a new range
     */
    @Transactional
    public void allocateNewRangeWithTransaction() {
        allocateNewRangeInternal();
    }

    /**
     * Internal method for range allocation logic
     * Note: Must be called from a @Transactional method
     */
    private void allocateNewRangeInternal() {
        synchronized (rangeLock) {
            try {
                log.info("Allocating new range for instance: {}", serviceInstanceId);

                // Get global counter (without pessimistic lock for simplicity)
                GlobalCounter counter = globalCounterRepository.findById(1)
                        .orElseGet(() -> {
                            // Create if doesn't exist
                            GlobalCounter newCounter = new GlobalCounter();
                            newCounter.setId(1);
                            newCounter.setCurrentCounter(0L);
                            newCounter.setRangeSize(rangeSize);
                            newCounter.setLastUpdated(LocalDateTime.now());
                            return globalCounterRepository.save(newCounter);
                        });

                // Allocate new range
                Long rangeStart = counter.getCurrentCounter();
                Long rangeEnd = rangeStart + rangeSize - 1;
                
                // Update counter for next allocation
                counter.setCurrentCounter(rangeEnd + 1);
                counter.setLastUpdated(LocalDateTime.now());
                globalCounterRepository.save(counter);

                // Create range allocation record
                RangeAllocation allocation = new RangeAllocation();
                allocation.setServiceInstanceId(serviceInstanceId);
                allocation.setStartRange(rangeStart);
                allocation.setEndRange(rangeEnd);
                allocation.setStatus(RangeAllocation.RangeStatus.ACTIVE);
                allocation.setAllocatedAt(LocalDateTime.now());
                rangeAllocationRepository.save(allocation);

                // Update local state
                this.currentCounter = new AtomicLong(rangeStart);
                this.rangeEnd = rangeEnd;

                log.info("Successfully allocated range [{}, {}] for instance {}", 
                        rangeStart, rangeEnd, serviceInstanceId);

            } catch (Exception e) {
                log.error("Failed to allocate range", e);
                throw new RuntimeException("Failed to allocate new range", e);
            }
        }
    }

    /**
     * Encode a number to Base62 string.
     * Base62 uses: 0-9, a-z, A-Z (62 characters total)
     * 
     * Examples:
     * 0 -> "0"
     * 61 -> "Z"
     * 62 -> "10"
     * 1000 -> "g8"
     * 
     * @param num The number to encode
     * @return Base62 encoded string
     */
    String encodeBase62(long num) {
        if (num == 0) {
            return String.valueOf(charset.charAt(0));
        }

        StringBuilder encoded = new StringBuilder();
        int base = charset.length();

        while (num > 0) {
            int remainder = (int) (num % base);
            encoded.insert(0, charset.charAt(remainder));
            num = num / base;
        }

        return encoded.toString();
    }

    /**
     * Decode a Base62 string back to a number.
     * Useful for testing and validation.
     * 
     * @param str Base62 encoded string
     * @return The decoded number
     */
    public long decodeBase62(String str) {
        long decoded = 0;
        int base = charset.length();

        for (char c : str.toCharArray()) {
            int value = charset.indexOf(c);
            if (value == -1) {
                throw new IllegalArgumentException("Invalid character in Base62 string: " + c);
            }
            decoded = decoded * base + value;
        }

        return decoded;
    }

    /**
     * Validate if a custom code is acceptable.
     * 
     * @param customCode The custom code to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidCustomCode(String customCode) {
        if (customCode == null || customCode.isEmpty()) {
            return false;
        }

        // Check length
        if (customCode.length() < 3 || customCode.length() > 50) {
            return false;
        }

        // Check characters (alphanumeric, dash, underscore only)
        return customCode.matches("^[a-zA-Z0-9_-]+$");
    }

    /**
     * Get current range info (for monitoring/debugging)
     */
    public String getRangeInfo() {
        if (currentCounter == null || rangeEnd == null) {
            return "Not initialized";
        }
        return String.format("Instance: %s, Current: %d, End: %d, Remaining: %d",
                serviceInstanceId,
                currentCounter.get(),
                rangeEnd,
                rangeEnd - currentCounter.get());
    }
}