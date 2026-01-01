# URL Shortener - Design Documentation

## Overview
This document explains the design decisions for the URL shortener database schema and architecture.

## Database Schema Design Decisions

### 1. **Users Table**
**Purpose**: Store registered user accounts

**Key Design Choices**:
- `email` is the primary login identifier (unique, indexed)
- `password_hash` uses bcrypt (handled in Java, never store plain text)
- `username` is optional but unique (for vanity URLs like mysite.com/u/johndoe)
- `is_active` for soft deletes (better than hard deletes for analytics preservation)

**Why this design**:
- Separates guest URLs (user_id = NULL) from authenticated user URLs
- Allows future features like "Claim your guest URLs" by setting user_id later
- Soft delete preserves referential integrity for analytics

### 2. **URLs Table** (Core Table)
**Purpose**: Store all shortened URLs (both guest and authenticated)

**Key Design Choices**:
- `short_code` is the generated hash (VARCHAR(20) to support longer codes if needed)
- `user_id` is nullable (NULL = guest, value = authenticated user)
- `click_count` is denormalized for performance (updated on each redirect)
- `metadata` as JSONB for flexible future features (tags, categories, notes)
- `expires_at` allows temporary URLs (NULL = permanent)

**Critical Indexes**:
```sql
-- Most important: lookup by short_code (happens on every redirect)
CREATE UNIQUE INDEX idx_urls_short_code ON urls(short_code) WHERE is_active = TRUE;

-- Partial index for the hot path (active, non-expired URLs)
CREATE INDEX idx_urls_active_non_expired ON urls(short_code, original_url) 
WHERE is_active = TRUE AND (expires_at IS NULL OR expires_at > NOW());
```

**Why JSONB for metadata**:
- Users might want to add tags: `{"tags": ["work", "important"]}`
- Custom data without schema changes
- PostgreSQL has excellent JSONB query support with GIN indexes

### 3. **Custom Aliases Table**
**Purpose**: Allow users to create memorable short codes like "my-blog" instead of random "x7Kp2"

**Key Design Choices**:
- Separate table (not a column in URLs) because:
  - One URL can have multiple aliases over time
  - Keeps the URL table simpler
  - Easier to validate custom code uniqueness
- `is_active` allows users to deactivate/change aliases without deleting

**Example Use Case**:
```
Original URL: https://myblog.com/article-123
Generated code: x7Kp2
Custom alias: my-blog-post
Both redirect to the same original_url
```

### 4. **Analytics Table**
**Purpose**: Track every click for analytics dashboard

**Key Design Choices**:
- **Separate from URLs table** (not a click_count column only)
- Stores detailed per-click data: IP, user agent, referer, location
- `device_info` as JSONB: `{"browser": "Chrome", "os": "MacOS", "device": "Desktop"}`
- No UPDATE operations, only INSERTs (write-optimized)

**Performance Consideration**:
- This table will grow FAST (one row per click)
- Indexed on `url_id + clicked_at DESC` for dashboard queries
- Future: Partition by date (e.g., one partition per month)
- Future: Archive old data to S3 after 1 year

**Sample Analytics Query**:
```sql
-- Get clicks per day for last 30 days
SELECT 
    DATE(clicked_at) as date,
    COUNT(*) as clicks,
    COUNT(DISTINCT ip_address) as unique_visitors,
    COUNT(DISTINCT country) as countries_reached
FROM analytics
WHERE url_id = 123 
  AND clicked_at > NOW() - INTERVAL '30 days'
GROUP BY DATE(clicked_at)
ORDER BY date DESC;
```

### 5. **Sessions Table**
**Purpose**: Manage user authentication with JWT tokens

**Key Design Choices**:
- Stores JWT tokens for server-side validation (alternative to stateless JWT)
- `expires_at` for automatic session expiration
- `ip_address` and `user_agent` for security audit trail
- Trigger on INSERT updates `users.last_login`

**Session Flow**:
1. User logs in → Generate JWT → Store in sessions table
2. Each request → Validate JWT exists and not expired
3. Logout → Set `is_active = FALSE`
4. Scheduled job runs `cleanup_expired_sessions()` daily

### 6. **Hash Range Allocations** (Advanced)
**Purpose**: Distributed ID generation without coordination

**The Problem**:
- Multiple Spring Boot instances running
- Each needs to generate unique short codes
- Can't query DB "is this code taken?" on every request (too slow)

**The Solution** - Pre-allocated Ranges:
```
Instance 1: Gets range 1-1000
Instance 2: Gets range 1001-2000
Instance 3: Gets range 2001-3000
```

Each instance:
- Maintains a local counter
- Generates codes from its range
- Requests a new range when running low
- **Zero coordination** between instances!

**How it Works**:
```sql
-- Instance 1 requests a range
BEGIN;
SELECT current_counter FROM global_counter FOR UPDATE; -- Lock row
-- Returns: 0
UPDATE global_counter SET current_counter = 1000;
INSERT INTO range_allocations 
VALUES ('instance-1', 0, 1000, NOW(), 'ACTIVE');
COMMIT;
-- Instance 1 now owns codes 0-1000
```

**Base62 Encoding**:
```
Counter: 1537
Base62: "Pr"  (using charset: 0-9, a-z, A-Z)
Short URL: mysite.com/Pr
```

**Why This Design**:
- **Scalability**: Add more instances without conflicts
- **Performance**: No DB lookup per code generation
- **Collision-free**: Each instance has exclusive range
- **Transparent**: URLs table doesn't know about ranges

### 7. **Global Counter Table**
**Purpose**: Single source of truth for next available range

**Key Design Choices**:
- **Single-row table** (enforced with `CHECK (id = 1)`)
- Atomic updates with `FOR UPDATE` row lock
- `range_size` configurable (default: 1000 codes per range)

**Why Single Row**:
- Simple atomic counter
- PostgreSQL row-level locks prevent race conditions
- Only accessed when allocating ranges (infrequent)

## Architecture Patterns

### Guest vs Authenticated Flow

**Guest URL Creation**:
```
POST /api/shorten
Body: {"url": "https://example.com"}
No authentication needed
Returns: {"shortCode": "x7Kp2", "shortUrl": "mysite.com/x7Kp2"}
```

**Authenticated URL Creation**:
```
POST /api/shorten
Headers: Authorization: Bearer <JWT>
Body: {"url": "https://example.com", "customCode": "my-link"}
Returns: {
  "shortCode": "x7Kp2",
  "customCode": "my-link", 
  "shortUrl": "mysite.com/my-link"
}
```

**User Dashboard Query**:
```sql
-- Get all URLs for user with analytics
SELECT 
    u.short_code,
    u.original_url,
    u.created_at,
    u.click_count,
    ca.custom_code,
    COUNT(a.id) as detailed_clicks,
    MAX(a.clicked_at) as last_click
FROM urls u
LEFT JOIN custom_aliases ca ON u.id = ca.url_id AND ca.is_active = TRUE
LEFT JOIN analytics a ON u.id = a.url_id
WHERE u.user_id = ? AND u.is_active = TRUE
GROUP BY u.id, ca.custom_code
ORDER BY u.created_at DESC;
```

### Caching Strategy

**Redis Cache Layout**:
```
Key: shortcode:x7Kp2
Value: https://example.com
TTL: 1 hour

Key: shortcode:my-link  (custom alias)
Value: https://myblog.com
TTL: 24 hours  (custom codes are more stable)

Key: hot:urls
Value: [list of top 100 most clicked URLs]
TTL: 5 minutes
```

**Cache Flow**:
```
1. Request: GET /x7Kp2
2. Check Redis: GET shortcode:x7Kp2
3. If HIT → Return URL, track analytics async
4. If MISS → Query Postgres, cache result, return
```

**Write-Through Pattern**:
```java
// When creating new URL
String shortCode = generateShortCode();
urlRepository.save(url); // Write to Postgres
redisTemplate.set("shortcode:" + shortCode, originalUrl, 1, TimeUnit.HOURS);
```

### Analytics Processing

**Async Analytics** (Important for Performance):
```
Redirect Request → Immediate 301 Response
                  ↓
           Background Job Queue
                  ↓
      Batch insert to analytics table
```

**Why Async**:
- User redirect happens in <50ms
- Analytics INSERT doesn't block response
- Can batch inserts (1000 at a time) for efficiency

**Implementation Options**:
```java
// Option 1: Spring @Async
@Async
public void trackAnalytics(Long urlId, HttpServletRequest request) {
    Analytics analytics = new Analytics();
    analytics.setUrlId(urlId);
    analytics.setIpAddress(request.getRemoteAddr());
    // ... parse user agent, etc.
    analyticsRepository.save(analytics);
}

// Option 2: Message Queue (RabbitMQ/SQS)
rabbitTemplate.convertAndSend("analytics-queue", analyticsEvent);
```

## Constraints & Validation

### Database-Level Constraints
```sql
-- Email format validation
CONSTRAINT email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')

-- URL must be HTTP/HTTPS
CONSTRAINT url_format CHECK (original_url ~* '^https?://')

-- Custom codes: alphanumeric, dash, underscore only
CONSTRAINT custom_code_format CHECK (custom_code ~* '^[a-zA-Z0-9_-]{3,50}$')

-- Prevent overlapping range allocations (PostgreSQL EXCLUDE constraint)
EXCLUDE USING gist (
    int8range(start_range, end_range, '[]') WITH &&
) WHERE (status = 'ACTIVE')
```

### Application-Level Validation (Java)
```java
// Spring Boot validation annotations
@Entity
public class Url {
    @Pattern(regexp = "^https?://.*", message = "Must be valid HTTP/HTTPS URL")
    private String originalUrl;
    
    @Pattern(regexp = "^[a-zA-Z0-9_-]{1,20}$")
    private String shortCode;
    
    @Min(0)
    private Integer clickCount;
}
```

## Performance Optimizations

### 1. Denormalization
```sql
-- Instead of COUNT(*) from analytics on every dashboard load
-- Maintain click_count in urls table
UPDATE urls SET click_count = click_count + 1 WHERE id = ?;

-- Trade-off: Slight inconsistency (analytics async) for huge read performance
```

### 2. Partial Indexes
```sql
-- Index only active URLs (90% of queries)
CREATE INDEX idx_urls_short_code ON urls(short_code) WHERE is_active = TRUE;

-- Saves space and increases index efficiency
```

### 3. JSONB GIN Indexes
```sql
-- For metadata queries like "find all URLs with tag 'work'"
CREATE INDEX idx_urls_metadata_gin ON urls USING gin(metadata);

-- Query:
SELECT * FROM urls WHERE metadata @> '{"tags": ["work"]}';
```

### 4. Connection Pooling (HikariCP)
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### 5. Read Replicas (Future)
```
Write Operations (INSERT, UPDATE) → Primary DB
Read Operations (SELECT) → Read Replica(s)

Spring Boot can route automatically with @Transactional(readOnly = true)
```

## Security Considerations

### 1. Rate Limiting
```java
// Spring Boot + Redis
@RateLimiter(name = "shortenUrl", fallbackMethod = "rateLimitFallback")
public ShortenedUrl createShortUrl(String url) { ... }

// Config: 10 requests per minute per IP
```

### 2. SQL Injection Prevention
```java
// NEVER:
String query = "SELECT * FROM urls WHERE short_code = '" + code + "'";

// ALWAYS use JPA or PreparedStatement:
@Query("SELECT u FROM Url u WHERE u.shortCode = :code")
Url findByShortCode(@Param("code") String code);
```

### 3. Password Security
```java
// Bcrypt with strength 12
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
String hashedPassword = encoder.encode(plainPassword);
```

### 4. Custom Code Abuse Prevention
```java
// Blacklist offensive words
List<String> blacklist = Arrays.asList("admin", "api", "login", "offensive-word");

// Validate before saving
if (blacklist.contains(customCode.toLowerCase())) {
    throw new InvalidCustomCodeException();
}
```

## Questions to Consider

Before implementing, think about:

1. **Custom Code Length**: 3-50 chars good? Or limit to 10 for aesthetics?
2. **URL Expiration**: Support temporary URLs? Default TTL?
3. **Guest Limits**: Should guests have rate limits but users don't?
4. **Analytics Retention**: Keep analytics forever or archive after 1 year?
5. **Range Size**: 1000 codes per instance enough? Or 10,000?
