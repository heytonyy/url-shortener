# URL Shortener - Full Stack Java Application

A production-ready URL shortener built with Spring Boot, PostgreSQL, Redis, and React. Features include custom aliases, analytics tracking, user authentication, and distributed short code generation.

## 🏗️ Architecture

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────┐
│   React     │────▶│  Spring Boot API │────▶│ PostgreSQL  │
│  Frontend   │     │   (REST API)     │     │  Database   │
└─────────────┘     └──────────────────┘     └─────────────┘
                            │
                            ▼
                    ┌──────────────┐
                    │    Redis     │
                    │   (Cache)    │
                    └──────────────┘
```

### Key Features

✅ **Guest & Authenticated Users**: Create URLs without login or with account for management  
✅ **Custom Aliases**: Users can create memorable short codes like `/my-blog`  
✅ **Analytics Tracking**: Track clicks, locations, devices, and referrers  
✅ **Distributed ID Generation**: Hash Range Allocator for collision-free short codes  
✅ **Redis Caching**: Fast redirects with intelligent caching  
✅ **JWT Authentication**: Secure user sessions  
✅ **Production Ready**: Metrics, logging, health checks included  

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker & Docker Compose

### 1. Start Database & Redis

```bash
# Start PostgreSQL and Redis
docker-compose up -d

# Verify containers are running
docker ps
```

Access pgAdmin at http://localhost:5050 (admin@admin.com / admin)

### 2. Build & Run Application

```bash
# Clean and build
mvn clean install

# Run application
mvn spring-boot:run

# Or run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The API will be available at: http://localhost:8080/api

### 3. Verify Installation

```bash
# Check health
curl http://localhost:8080/api/actuator/health

# Check metrics
curl http://localhost:8080/api/actuator/prometheus
```

## 📁 Project Structure

```
url-shortener/
├── src/
│   ├── main/
│   │   ├── java/com/urlshortener/api/
│   │   │   ├── controller/          # REST API endpoints
│   │   │   ├── service/             # Business logic
│   │   │   ├── repository/          # Database access
│   │   │   ├── entity/              # JPA entities
│   │   │   ├── dto/                 # Data transfer objects
│   │   │   ├── config/              # Spring configuration
│   │   │   ├── security/            # Authentication & authorization
│   │   │   ├── exception/           # Custom exceptions
│   │   │   └── util/                # Utility classes
│   │   └── resources/
│   │       ├── application.yml      # Configuration
│   │       └── db/migration/        # Flyway migrations
│   └── test/                        # Unit & integration tests
├── docker-compose.yml               # Local development environment
├── pom.xml                          # Maven dependencies
└── README.md
```

## 🗄️ Database Schema

### Core Tables

- **users**: Registered user accounts
- **urls**: Shortened URLs (guest + authenticated)
- **custom_aliases**: User-defined short codes
- **analytics**: Click tracking data
- **sessions**: JWT token management

### Distributed ID Generation

- **range_allocations**: Tracks allocated code ranges per service instance
- **global_counter**: Single-row table for atomic range allocation

### Sample Queries

```sql
-- Find user's URLs with analytics
SELECT 
    u.short_code,
    u.original_url,
    u.click_count,
    ca.custom_code,
    COUNT(a.id) as detailed_clicks
FROM urls u
LEFT JOIN custom_aliases ca ON u.id = ca.url_id
LEFT JOIN analytics a ON u.id = a.url_id
WHERE u.user_id = ? AND u.is_active = true
GROUP BY u.id, ca.custom_code;

-- Get clicks by date (last 30 days)
SELECT 
    DATE(clicked_at) as date,
    COUNT(*) as clicks
FROM analytics
WHERE url_id = ? AND clicked_at > NOW() - INTERVAL '30 days'
GROUP BY DATE(clicked_at)
ORDER BY date DESC;
```

## 🔧 Configuration

### Application Properties (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/urlshortener
    username: admin
    password: password

  data:
    redis:
      host: localhost
      port: 6379

app:
  shortcode:
    length: 6
    charset: "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
  
  range:
    size: 1000        # Codes per range allocation
    threshold: 100    # Request new range when this many remain
    
  url:
    base-url: http://localhost:8080
```

### Environment Variables (Production)

```bash
DATABASE_URL=jdbc:postgresql://your-rds-endpoint:5432/urlshortener
DATABASE_USERNAME=your-username
DATABASE_PASSWORD=your-password
REDIS_HOST=your-elasticache-endpoint
REDIS_PORT=6379
JWT_SECRET=your-256-bit-secret-key
BASE_URL=https://yourdomain.com
```

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ShortCodeGeneratorTest

# Run integration tests
mvn verify
```

## 📊 API Endpoints

### Public Endpoints (No Authentication)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/shorten` | Create short URL (guest) |
| GET | `/{shortCode}` | Redirect to original URL |

### Authenticated Endpoints (JWT Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login and get JWT token |
| POST | `/api/shorten` | Create URL with custom alias |
| GET | `/api/users/me/urls` | Get user's URLs |
| PUT | `/api/urls/{id}/alias` | Update custom alias |
| GET | `/api/analytics/{urlId}` | Get URL analytics |
| DELETE | `/api/urls/{id}` | Deactivate URL |

## 🎯 Next Steps

### Phase 1: Complete Core API (Week 2)
- [ ] Implement UrlService (create, redirect, analytics)
- [ ] Create REST controllers
- [ ] Add authentication with JWT
- [ ] Write unit tests for services

### Phase 2: Frontend (Week 3)
- [ ] Create React app with Vite
- [ ] Build URL shortening form
- [ ] User dashboard for managing URLs
- [ ] Analytics visualization

### Phase 3: AWS Deployment (Week 4-5)
- [ ] Containerize with Docker
- [ ] Set up RDS PostgreSQL
- [ ] Deploy to ECS Fargate
- [ ] Configure Application Load Balancer
- [ ] Set up CloudWatch monitoring

### Phase 4: Advanced Features (Week 6)
- [ ] QR code generation
- [ ] Bulk URL shortening
- [ ] URL expiration notifications
- [ ] Rate limiting
- [ ] API documentation with Swagger

## 🔥 Hash Range Allocator Algorithm

The core innovation of this project is the distributed short code generation:

### How It Works

1. **On Startup**: Each service instance requests a range (e.g., 0-999)
2. **Local Counter**: Maintains atomic counter within allocated range
3. **Base62 Encoding**: Converts counter to short code (1537 → "Pr")
4. **Auto-Refill**: Requests new range when threshold reached
5. **Zero Coordination**: No inter-service communication needed

### Example

```
Instance 1: Range 0-999     → Generates: 0, 1, 2, ...
Instance 2: Range 1000-1999 → Generates: 1000, 1001, 1002, ...

Counter 1537 → Base62 encode → "Pr"
Short URL: https://mysite.com/Pr
```

### Benefits

✅ No collisions between instances  
✅ No database lookups per code generation  
✅ Horizontal scalability  
✅ Predictable performance  

## 🛠️ Technologies Used

- **Backend**: Spring Boot 3.2, Java 17
- **Database**: PostgreSQL 14 with JSONB support
- **Cache**: Redis 7
- **ORM**: Spring Data JPA (Hibernate)
- **Migrations**: Flyway
- **Security**: Spring Security + JWT
- **Metrics**: Micrometer + Prometheus
- **Testing**: JUnit 5, MockMvc, RestAssured

## 📝 Development Tips

### Database Migrations

```bash
# Create new migration
# Create file: src/main/resources/db/migration/V2__Add_feature.sql

# Flyway will auto-run on startup

# Manually trigger migration
mvn flyway:migrate
```

### Redis CLI

```bash
# Connect to Redis
docker exec -it url-shortener-redis redis-cli

# Check cached URLs
KEYS shortcode:*
GET shortcode:abc123

# Clear cache
FLUSHALL
```

### Logs

```bash
# View application logs
tail -f logs/url-shortener.log

# View SQL queries (set logging.level.org.hibernate.SQL=DEBUG)
```

## 🤝 Contributing

This is a personal learning project, but feedback is welcome!

## 📄 License

MIT License

---

**Happy Coding! 🚀**

For questions or issues, check the GitHub discussions or open an issue.