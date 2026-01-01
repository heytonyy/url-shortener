# Next Steps - Implementation Guide

## 🎯 Current Status

✅ **Completed:**
- Database schema designed with all tables
- Flyway migration ready (V1__Initial_Schema.sql)
- All JPA entities created (User, Url, CustomAlias, Analytics, Session, RangeAllocation, GlobalCounter)
- All repositories created with custom queries
- ShortCodeGenerator service (core algorithm)
- Project structure with Maven
- Docker Compose for local development
- Application configuration (application.yml)

## 📋 What's Next (In Order)

### Step 1: Test the ShortCodeGenerator (30 minutes)

Create a test class to verify the Base62 encoding and range allocation works:

**Create:** `src/test/java/com/urlshortener/api/service/ShortCodeGeneratorTest.java`

```java
@SpringBootTest
class ShortCodeGeneratorTest {
    
    @Autowired
    private ShortCodeGenerator generator;
    
    @Test
    void testBase62Encoding() {
        // Test encoding
        assertEquals("0", generator.encodeBase62(0));
        assertEquals("a", generator.encodeBase62(10));
        assertEquals("A", generator.encodeBase62(36));
        
        // Test decoding
        assertEquals(0, generator.decodeBase62("0"));
        assertEquals(10, generator.decodeBase62("a"));
    }
    
    @Test
    void testGenerateShortCode() {
        String code1 = generator.generateShortCode();
        String code2 = generator.generateShortCode();
        
        assertNotNull(code1);
        assertNotNull(code2);
        assertNotEquals(code1, code2); // Should be unique
    }
}
```

**Run:** `mvn test -Dtest=ShortCodeGeneratorTest`

### Step 2: Implement UrlService (2-3 hours)

This is the main business logic for creating and managing URLs.

**Create:** `src/main/java/com/urlshortener/api/service/UrlService.java`

**Key Methods:**
```java
@Service
public class UrlService {
    
    // Create short URL for guest
    ShortenedUrlResponse createShortUrl(String originalUrl);
    
    // Create short URL with custom alias for authenticated user
    ShortenedUrlResponse createShortUrl(String originalUrl, String customCode, Long userId);
    
    // Redirect - get original URL by short code
    String getOriginalUrl(String shortCode);
    
    // Get all URLs for a user
    List<UrlResponse> getUserUrls(Long userId);
    
    // Update custom alias
    CustomAliasResponse updateCustomAlias(Long urlId, String customCode, Long userId);
    
    // Deactivate URL
    void deactivateUrl(Long urlId, Long userId);
}
```

**Implementation Tips:**
- Use `@Transactional` for methods that write to database
- Use `@Cacheable` for `getOriginalUrl()` - cache in Redis
- Validate custom codes before creating
- Check if custom code already exists
- Increment click count asynchronously

### Step 3: Implement AnalyticsService (1-2 hours)

**Create:** `src/main/java/com/urlshortener/api/service/AnalyticsService.java`

```java
@Service
public class AnalyticsService {
    
    // Track a click (should be async)
    @Async
    void trackClick(Long urlId, HttpServletRequest request);
    
    // Get analytics for a URL
    UrlAnalyticsResponse getUrlAnalytics(Long urlId, Long userId);
    
    // Get clicks by date for last N days
    List<ClicksByDateResponse> getClicksByDate(Long urlId, int days);
    
    // Get top countries
    List<CountryStatsResponse> getTopCountries(Long urlId);
}
```

**Device Info Extraction:**
You'll need to parse the User-Agent header. Consider using a library like `user-agent-utils` or `ua-parser`.

### Step 4: Create DTOs (1 hour)

Create Data Transfer Objects for API requests/responses:

**Create these files in `src/main/java/com/urlshortener/api/dto/`:**

```
ShortenUrlRequest.java
ShortenedUrlResponse.java
UrlResponse.java
CustomAliasResponse.java
UrlAnalyticsResponse.java
ClicksByDateResponse.java
CountryStatsResponse.java
RegisterRequest.java
LoginRequest.java
AuthResponse.java
```

**Example:**
```java
@Data
@Builder
public class ShortenUrlRequest {
    @NotBlank
    @Pattern(regexp = "^https?://.*")
    private String url;
    
    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,50}$")
    private String customCode;  // Optional
}

@Data
@Builder
public class ShortenedUrlResponse {
    private String shortCode;
    private String customCode;  // If created
    private String shortUrl;    // Full URL: http://localhost:8080/abc
    private String originalUrl;
    private LocalDateTime createdAt;
}
```

### Step 5: Implement REST Controllers (2-3 hours)

**Create these controllers:**

**5.1: UrlController** - `src/main/java/com/urlshortener/api/controller/UrlController.java`

```java
@RestController
@RequestMapping("/api")
public class UrlController {
    
    @PostMapping("/shorten")
    public ResponseEntity<ShortenedUrlResponse> shortenUrl(
        @Valid @RequestBody ShortenUrlRequest request,
        @AuthenticationPrincipal UserDetails userDetails  // Null if guest
    );
    
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
        @PathVariable String shortCode,
        HttpServletRequest request
    );
    
    @GetMapping("/users/me/urls")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UrlResponse>> getUserUrls(
        @AuthenticationPrincipal UserDetails userDetails
    );
    
    @PutMapping("/urls/{id}/alias")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CustomAliasResponse> updateAlias(
        @PathVariable Long id,
        @RequestBody UpdateAliasRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    );
    
    @DeleteMapping("/urls/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deactivateUrl(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails
    );
}
```

**5.2: AuthController** - For registration and login

**5.3: AnalyticsController** - For analytics endpoints

### Step 6: Implement Security (2-3 hours)

**Create:** `src/main/java/com/urlshortener/api/security/`

**Files needed:**
- `JwtTokenProvider.java` - Generate and validate JWT tokens
- `JwtAuthenticationFilter.java` - Filter to validate JWT on each request
- `SecurityConfig.java` - Configure Spring Security
- `UserDetailsServiceImpl.java` - Load user for authentication

**SecurityConfig.java example:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .csrf().disable()
            .authorizeHttpRequests()
                .requestMatchers("/api/auth/**", "/api/shorten", "/{shortCode}").permitAll()
                .requestMatchers("/api/users/**").authenticated()
                .anyRequest().permitAll()
            .and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

### Step 7: Error Handling (1 hour)

**Create:** `src/main/java/com/urlshortener/api/exception/`

```java
GlobalExceptionHandler.java
UrlNotFoundException.java
CustomCodeAlreadyExistsException.java
UnauthorizedException.java
InvalidUrlException.java
```

**GlobalExceptionHandler:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUrlNotFound(UrlNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }
    
    // ... other handlers
}
```

### Step 8: Testing with Postman/cURL (1 hour)

**Test Guest URL Creation:**
```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.google.com"}'

# Response: 
# {
#   "shortCode": "a",
#   "shortUrl": "http://localhost:8080/a",
#   "originalUrl": "https://www.google.com"
# }
```

**Test Redirect:**
```bash
curl -L http://localhost:8080/a
# Should redirect to https://www.google.com
```

**Test User Registration:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "username": "testuser"
  }'
```

**Test User Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'

# Response: {"token": "eyJhbG..."}
```

**Test Create URL with Custom Alias:**
```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "url": "https://www.example.com",
    "customCode": "my-blog"
  }'
```

## 🎓 Learning Objectives Checklist

As you implement each piece, make sure you understand:

**Java & Spring Boot:**
- [ ] Dependency Injection with `@Autowired`
- [ ] `@Transactional` and transaction management
- [ ] `@Async` for asynchronous processing
- [ ] `@Cacheable` for Redis caching
- [ ] Request validation with `@Valid`
- [ ] Exception handling with `@ControllerAdvice`

**SQL & PostgreSQL:**
- [ ] Complex JOIN queries for analytics
- [ ] JSONB operations for metadata
- [ ] Indexes and query optimization
- [ ] Pessimistic locking with `SELECT FOR UPDATE`
- [ ] Flyway migrations

**DevOps (Later):**
- [ ] Docker containerization
- [ ] Environment-specific configuration
- [ ] AWS RDS setup
- [ ] ECS deployment
- [ ] Load balancer configuration

## 🐛 Common Issues & Solutions

**Issue 1: "Global counter not found"**
- Make sure the Flyway migration ran successfully
- Check `docker exec -it url-shortener-db psql -U admin -d urlshortener -c "SELECT * FROM global_counter;"`

**Issue 2: Redis connection refused**
- Verify Redis is running: `docker ps`
- Check Redis: `docker exec -it url-shortener-redis redis-cli ping`

**Issue 3: JWT token validation fails**
- Make sure the secret key in application.yml is at least 256 bits (32 characters)
- Verify the token is passed in the `Authorization: Bearer <token>` header

**Issue 4: Flyway migration errors**
- Clean the database: `docker-compose down -v` then `docker-compose up -d`
- Or manually: `mvn flyway:clean` then `mvn flyway:migrate`

## 📚 Recommended Order of Implementation

**Day 2 (Today):**
1. ✅ Get Docker containers running
2. ✅ Verify Flyway migration
3. ✅ Test ShortCodeGenerator
4. Start UrlService implementation

**Day 3:**
1. Finish UrlService
2. Implement DTOs
3. Create UrlController
4. Test with Postman

**Day 4:**
1. Implement AuthService
2. Create AuthController
3. Set up Spring Security
4. Test authentication flow

**Day 5:**
1. Implement AnalyticsService
2. Add AnalyticsController
3. Test analytics tracking
4. Write unit tests

**Day 6-7:**
1. Add error handling
2. Improve validation
3. Add integration tests
4. Create documentation

## 🎯 Success Criteria

You'll know you're ready to move to the frontend when:

✅ Can create short URLs as a guest  
✅ Can register and login  
✅ Can create URLs with custom aliases  
✅ Redirects work correctly  
✅ Analytics are being tracked  
✅ Users can view their URLs  
✅ All endpoints return proper error messages  
✅ Basic tests are passing  

---

**Ready to code? Start with Step 1! 🚀**

If you get stuck on any step, ask for help with specific code examples. Good luck!