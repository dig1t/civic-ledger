#!/bin/bash

# Start the Spring Boot backend in development mode
cd "$(dirname "$0")/.." || exit 1

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
