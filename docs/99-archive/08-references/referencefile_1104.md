```version: "3.9"

services:
eureka-server:
image: openjdk:17
container_name: eureka-server
working_dir: /app
volumes:
- ./eureka-server/build/libs:/app
command: ["java", "-jar", "eureka-server-0.0.1-SNAPSHOT.jar"]
ports:
- "8761:8761"
networks:
- ofl-net

gateway:
image: openjdk:17
container_name: gateway
working_dir: /app
volumes:
- ./gateway/build/libs:/app
command: ["java", "-jar", "gateway-0.0.1-SNAPSHOT.jar"]
ports:
- "8000:8000"
depends_on:
- eureka-server
networks:
- ofl-net

hub-service:
image: openjdk:17
container_name: hub-service
working_dir: /app
volumes:
- ./hub-service/build/libs:/app
command: ["java", "-jar", "hub-service-0.0.1-SNAPSHOT.jar"]
ports:
- "8200:8200"
depends_on:
- eureka-server
- postgres
- redis
networks:
- ofl-net

postgres:
image: postgres:17
container_name: postgres-ofl
environment:
POSTGRES_USER: postgres
POSTGRES_PASSWORD: password
POSTGRES_DB: ofl_hub
ports:
- "5432:5432"
volumes:
- postgres_data:/var/lib/postgresql/data
restart: unless-stopped
networks:
- ofl-net

redis:
image: redis:7
container_name: redis
command: ["redis-server", "--appendonly", "yes"]
ports:
- "6379:6379"
restart: unless-stopped
volumes:
- redis_data:/data
networks:
- ofl-net

networks:
ofl-net:

volumes:
postgres_data:
redis_data:```
---
```plugins {
    id ‘org.springframework.boot’ version ‘3.3.2’
    id ‘io.spring.dependency-management’ version ‘1.1.5’
    id ‘java’
}
group = ‘com.oneforlogis.company’
version = ‘0.0.1-SNAPSHOT’
sourceCompatibility = ‘17’
repositories {
    mavenCentral()
}
dependencies {
    implementation ‘org.springframework.boot:spring-boot-starter-web’
    implementation ‘org.springframework.cloud:spring-cloud-starter-netflix-eureka-client’
    testImplementation ‘org.springframework.boot:spring-boot-starter-test’
}
tasks.named(‘test’) {
    useJUnitPlatform()
}```
