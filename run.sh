#!/bin/bash

# Clean everything
mvn clean

# Restart Docker to reset database
docker-compose down -v
docker-compose up -d

# Wait a few seconds for PostgreSQL to start
sleep 5

# Run the application
mvn spring-boot:run