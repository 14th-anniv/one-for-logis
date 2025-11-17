# Sparta Logistics - MSA Planning Document

## Language: Korean responses only

## Code Rules
- Always ask approval before create/modify files
- Explain changes & reasons first
- Write any md file in English

## Git Commit
- Simple messages only
- NO "🤖 Generated with Claude Code" or "Co-Authored-By: Claude"
- NEVER commit CLAUDE.md or pr.md

## 1. Project Overview

**Project Name**: 14logis (one for logis)
**Type**: B2B Logistics Management and Delivery System
**Architecture**: Microservices Architecture (MSA)
**Environment**: Local development with Docker containers

---

## 2. Microservice Design

### 2.1 Service Boundaries

| Service | Responsibility | Database Schema | Port |
|---------|---------------|-----------------|------|
| **eureka-server** | Service Discovery | N/A | 8761 |
| **gateway-service** | API Gateway, Auth validation, Routing | N/A | 8080 |
| **auth-service** | User management, Authentication, JWT | auth_db | 8081 |
| **hub-service** | Hub management, Hub routes, Caching | hub_db | 8082 |
| **company-service** | Company management (Supplier/Receiver) | company_db | 8083 |
| **product-service** | Product management, Inventory | product_db | 8084 |
| **order-service** | Order creation, Order management | order_db | 8085 |
| **delivery-service** | Delivery tracking, Route records, Delivery personnel | delivery_db | 8086 |
| **slack-service** | Slack message management, Notifications | slack_db | 8087 |

### 2.2 Service Dependencies

```
gateway-service
  ├── auth-service (JWT validation)
  └── All other services (routing)

order-service
  ├── auth-service (user validation)
  ├── company-service (company validation)
  ├── product-service (product/inventory check)
  ├── hub-service (hub validation)
  ├── delivery-service (create delivery)
  └── slack-service (notifications)

delivery-service
  ├── hub-service (hub routes, hub info)
  ├── auth-service (delivery personnel info)
  └── slack-service (notifications)

product-service
  ├── company-service (company validation)
  └── hub-service (hub validation)

company-service
  └── hub-service (hub validation)
```

---

## 3. Database Design Strategy

### 3.1 Database Separation
- PostgreSQL with **separate schemas** per microservice
- Each service owns its data
- No direct database access between services
- All inter-service data access via REST API

### 3.2 Common Table Patterns

All tables include:
- **Naming**: `p_` prefix (e.g., `p_users`, `p_hubs`)
- **ID**: UUID (except users table)
- **Audit Fields**:
  - `created_at TIMESTAMP`
  - `created_by VARCHAR(100)`
  - `updated_at TIMESTAMP`
  - `updated_by VARCHAR(100)`
  - `deleted_at TIMESTAMP` (soft delete)
  - `deleted_by VARCHAR(100)` (soft delete)

### 3.3 Entities by Service

#### auth-service: `auth_db` schema
- `p_users` - User management and authentication

#### hub-service: `hub_db` schema
- `p_hubs` - Hub information (17 centers)
- `p_hub_routes` - Hub-to-hub route information

#### company-service: `company_db` schema
- `p_companies` - Company information (supplier/receiver)

#### product-service: `product_db` schema
- `p_products` - Product information
- `p_inventory` - Inventory per hub (optional)

#### order-service: `order_db` schema
- `p_orders` - Order information

#### delivery-service: `delivery_db` schema
- `p_delivery_personnel` - Delivery personnel (hub/company)
- `p_deliveries` - Overall delivery status
- `p_delivery_routes` - Hub-to-hub delivery route records

#### slack-service: `slack_db` schema
- `p_slack_messages` - Slack message history

---

## 4. User Roles & Permissions

### 4.1 Role Types

| Role | Code | Description |
|------|------|-------------|
| Master Admin | `MASTER` | Full system access |
| Hub Manager | `HUB_MANAGER` | Manages specific hub |
| Delivery Manager | `DELIVERY_MANAGER` | Hub or Company delivery personnel |
| Company Manager | `COMPANY_MANAGER` | Manages specific company |

### 4.2 Permission Matrix

| Resource | MASTER | HUB_MANAGER | DELIVERY_MANAGER | COMPANY_MANAGER |
|----------|--------|-------------|------------------|-----------------|
| **Hub** | CRUD + Search | Search only | Search only | Search only |
| **Hub Routes** | CRUD + Search | Search only | Search only | Search only |
| **Delivery Personnel** | CRUD + Search | CRUD (own hub) + Search (own hub) | Read (self) | None |
| **Company** | CRUD + Search | CRUD (own hub) + Search | Read | Update (self) + Search |
| **Product** | CRUD + Search | CRUD (own hub) + Search | Read | CRUD (self) + Search |
| **Order** | CRUD + Search | Update/Delete (own hub) + Search (own hub) | Read (self) | Create + Read (self) |
| **Delivery** | CRUD + Search | Update/Delete (own hub) + Search (own hub) | Update (self) + Read (self) | Read |
| **User** | CRUD + Search | Read (self) | Read (self) | Read (self) |
| **Slack Message** | Create + CRUD + Search | Create | Create | Create |

---

## 5. Authentication & Authorization Flow

### 5.1 User Registration (Approval-based)
1. User submits registration request → **auth-service**
2. Status set to `PENDING`
3. Required fields:
   - username (4-10 chars, lowercase + numbers)
   - password (8-15 chars, mixed case + numbers + special)
   - name
   - slack_id
   - affiliated company/hub name
4. MASTER or HUB_MANAGER reviews and APPROVES/REJECTS
5. Only `APPROVED` users can login

### 5.2 Login Flow
1. User sends credentials → **gateway-service** → **auth-service**
2. Auth-service validates and generates JWT
3. JWT contains: username, role, hub_id (if applicable), company_id (if applicable)
4. Return JWT to client

### 5.3 Request Authorization
1. All requests pass through **gateway-service**
2. Gateway extracts JWT and validates
3. Gateway adds user info to request headers
4. Each service checks permissions based on role + resource ownership

---

## 6. Hub Route Models (Choose One)

### Option 1: P2P (Point-to-Point) - Difficulty: LOW
- All hubs directly connected
- Direct delivery to any hub
- Simple but many routes to manage (17x16 = 272 routes)

### Option 2: Hub-and-Spoke - Difficulty: MEDIUM
- 3 central hubs: 경기남부, 대전광역시, 대구광역시
- All deliveries pass through central hubs
- More efficient resource usage
- Hub assignments:
  - 경기남부 (5): 경기북부, 서울, 인천, 경기남부, 강원도
  - 대전 (7): 충청남도, 충청북도, 세종, 대전, 전라북도, 광주, 전라남도
  - 대구 (5): 경상북도, 대구, 경상남도, 부산, 울산

### Option 3: P2P + Hub-to-Hub Relay - Difficulty: MEDIUM
- Adjacent hubs directly connected
- Long distance (>200km) requires relay points
- Need algorithm to find optimal relay points

### Option 4: Hub-to-Hub Relay - Difficulty: HIGH
- Only connected hubs can communicate
- Requires path-finding algorithm (Dijkstra)
- Most flexible but complex

**Recommendation**: Start with **Hub-and-Spoke** for balance of simplicity and realism.

---

## 7. Key Business Flows

### 7.1 Order Creation Flow
1. User creates order → **order-service**
2. Order-service validates:
   - Supplier company exists (**company-service**)
   - Receiver company exists (**company-service**)
   - Product exists (**product-service**)
   - Inventory sufficient (**product-service**)
3. Order-service creates order
4. Order-service calls **delivery-service** to create delivery
5. Delivery-service:
   - Calculates hub route (origin hub → destination hub)
   - Assigns delivery personnel (sequential assignment)
   - Creates delivery + delivery route records
6. Order-service calls **slack-service** with Gemini AI prompt
7. Slack-service:
   - Calls Gemini AI for delivery deadline calculation
   - Sends Slack notification to origin hub manager

### 7.2 Delivery Route Calculation
1. Get origin hub (supplier's hub)
2. Get destination hub (receiver's hub)
3. Query **hub-service** for route path
4. For each hub-to-hub segment:
   - Assign hub delivery personnel (sequential)
   - Create delivery route record
   - Set expected distance/time
5. Final segment (destination hub → receiver):
   - Assign company delivery personnel from destination hub
   - Update delivery record

### 7.3 Delivery Personnel Assignment
- Both hub and company delivery personnel
- Sequential assignment by `delivery_sequence` field
- Round-robin: sequence 0 → 1 → ... → 9 → 0
- New personnel get highest sequence + 1
- Deleted personnel sequences are NOT rearranged

---

## 8. Caching Strategy

### 8.1 Cache Targets
- **Hub information** (rarely changes)
- **Hub route information** (rarely changes)

### 8.2 Cache Implementation
- Use Spring Cache with Redis (optional) or Caffeine
- Cache eviction on UPDATE/DELETE operations
- TTL: 24 hours for hub data

---

## 9. External API Integration

### 9.1 Gemini AI Integration
**Purpose**: Calculate optimal delivery departure time
**Trigger**: Order creation
**Input to AI**:
- Product info + quantity
- Delivery request (deadline date/time)
- Origin, waypoints, destination
- Delivery personnel work hours (09:00-18:00)

**Output from AI**:
- Latest departure time to meet deadline
- (Optional) Additional logistics info

### 9.2 Slack API Integration
**Purpose**: Send notifications to delivery personnel
**Messages**:
- Order creation → Notify origin hub manager
- (Challenge) Daily 06:00 → Notify company delivery personnel with route

---

## 10. Search & Pagination

### 10.1 Search Requirements
- All entities support CRUD + Search
- Search filters: entity-specific
- Sort options: `created_at`, `updated_at` (ASC/DESC)
- Pagination: 10, 30, 50 items per page (default: 10)

### 10.2 Soft Delete
- All queries filter `WHERE deleted_at IS NULL`
- DELETE operations set `deleted_at` and `deleted_by`
- No physical deletion

---

## 11. Technology Stack

### 11.1 Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **Build Tool**: Gradle
- **Database**: PostgreSQL 15+
- **ORM**: Spring Data JPA

### 11.2 MSA Infrastructure
- **Service Discovery**: Spring Cloud Netflix Eureka
- **API Gateway**: Spring Cloud Gateway
- **Service Communication**: OpenFeign
- **Distributed Tracing**: Zipkin
- **API Documentation**: Springdoc OpenAPI (Swagger)

### 11.3 Security
- **Authentication**: JWT
- **Authorization**: Spring Security
- **Password Hashing**: BCrypt

### 11.4 Deployment
- **Containerization**: Docker
- **Orchestration**: Docker Compose
- **Environment**: Local development only

---

## 12. API Design Principles

### 12.1 RESTful Conventions
- GET: Retrieve resources
- POST: Create resources
- PUT/PATCH: Update resources
- DELETE: Soft delete resources

### 12.2 Request/Response Format
```json
// Request Headers
{
  "Authorization": "Bearer {jwt_token}",
  "Content-Type": "application/json"
}

// Success Response
{
  "data": { ... },
  "message": "Success"
}

// Error Response
{
  "error": "Error message",
  "code": "ERROR_CODE",
  "timestamp": "2025-10-31T10:00:00"
}
```

### 12.3 Base URL Pattern
```
http://localhost:8080/api/{service}/{resource}

Examples:
- http://localhost:8080/api/hubs
- http://localhost:8080/api/companies
- http://localhost:8080/api/orders
```

---

## 13. Testing Strategy

### 13.1 Unit Tests
- Service layer logic
- Domain validation
- Utility functions

### 13.2 Integration Tests
- API endpoint tests
- Database integration
- FeignClient mocking

### 13.3 E2E Tests
- Full business flow tests
- Order creation → Delivery creation → Notification

---

## 14. Documentation Requirements

### 14.1 ERD (Entity Relationship Diagram)
- Tool: https://dbdiagram.io/ or https://app.diagrams.net/
- One diagram per microservice
- Show relationships (even if cross-service)

### 14.2 Infrastructure Diagram
- Tool: https://app.diagrams.net/
- Show all services, databases, external APIs
- Include communication flows

### 14.3 API Specification
- Format: Swagger/OpenAPI
- Include: method, URL, headers, request/response examples, role requirements
- Per-service documentation
- Aggregated view via Gateway (optional, bonus)

### 14.4 Table Specification
- All tables with field names, types, descriptions
- Follow provided user table example

---

## 15. Challenge Features (Optional)

### 15.1 Message Queue Integration
- Replace some API calls with async messaging
- Options: Kafka, RabbitMQ
- Use cases: Order creation events, Slack notifications

### 15.2 Optimized Daily Route Planning
- Daily 06:00 Slack notification to company delivery personnel
- Use Gemini AI to optimize visit order
- Integrate Naver Maps Directions 5 API
- Calculate optimal waypoints and route

### 15.3 Advanced Delivery Personnel Assignment
- Beyond simple sequential assignment
- Consider: distance, workload, availability
- Optimize for efficiency

---

## 16. Development Workflow

### 16.1 Git Strategy
- Feature branch workflow
- Branch naming: `feature/{service-name}/{feature-name}`
- PR review required before merge

### 16.2 Code Conventions
- Java: Google Java Style Guide
- Package structure: DDD-style (application, domain, infrastructure, presentation)
- Naming: Clear, descriptive names

### 16.3 PR Review Checklist
- Code follows conventions
- Tests included and passing
- API documentation updated
- No security vulnerabilities

---

## 17. Next Steps

### Phase 1: Planning & Design
- [ ] Finalize hub route model selection
- [ ] Complete ERD design for all services
- [ ] Create infrastructure architecture diagram
- [ ] Write detailed API specifications

### Phase 2: Infrastructure Setup
- [ ] Create multi-module Gradle project
- [ ] Setup Eureka Server
- [ ] Setup API Gateway
- [ ] Create Docker Compose configuration
- [ ] Setup PostgreSQL with schemas

### Phase 3: Core Service Development
- [ ] Implement auth-service
- [ ] Implement hub-service
- [ ] Implement company-service
- [ ] Implement product-service
- [ ] Implement order-service
- [ ] Implement delivery-service
- [ ] Implement slack-service

### Phase 4: Integration & Testing
- [ ] Test inter-service communication
- [ ] Test authentication/authorization
- [ ] Test business flows
- [ ] Integrate Gemini AI
- [ ] Integrate Slack API

### Phase 5: Documentation & Polish
- [ ] Complete Swagger documentation
- [ ] Setup Zipkin tracing
- [ ] Write README
- [ ] Prepare demo data

---

## 18. Open Questions & Decisions Needed

1. **Hub Route Model**: Which model to implement? (Recommend: Hub-and-Spoke)
2. **Inventory Management**: Separate inventory table or field in product table?
3. **Delivery Personnel**: Hub-less personnel (for hub delivery) - how to store hub_id?
4. **Transaction Management**: How to handle distributed transactions? (Saga pattern?)
5. **API Gateway Auth**: Full auth in gateway vs delegated to auth-service?
6. **Message Queue**: Use from start or add later as challenge?

---

## Notes
- Focus on MSA architecture and service communication
- Start simple, iterate
- Document decisions and trade-offs
- Test locally with Docker before any deployment consideration
