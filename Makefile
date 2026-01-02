.PHONY: run test clean docker-up docker-down docker-reset help

run:
	mvn spring-boot:run

test:
	mvn test

clean:
	mvn clean

docker-up:
	docker-compose up -d

docker-down:
	docker-compose down -v

docker-reset:
	docker-compose down -v
	docker-compose up -d
	@echo "Waiting for PostgreSQL to start..."
	@sleep 5
	@echo "Docker containers reset complete!"

# Combined workflows
clean-run: clean docker-reset
	mvn spring-boot:run

dev: clean docker-reset run

help:
	@echo "Available targets:"
	@echo "  make run           - Run Spring Boot application"
	@echo "  make test          - Run tests"
	@echo "  make clean         - Clean Maven build"
	@echo "  make docker-up     - Start Docker containers"
	@echo "  make docker-down   - Stop Docker containers"
	@echo "  make docker-reset  - Reset Docker containers (down + up)"
	@echo "  make clean-run     - Clean, reset Docker, and run"
	@echo "  make dev           - Full dev workflow (clean + reset + run)"
	@echo "  make help          - Show this help message"