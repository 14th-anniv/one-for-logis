# Business Rules & Domain Logic

This document describes the core business logic, workflows, and domain rules for the 14logis project.

**Last Updated**: 2025-11-06

---

## Project Requirements

### 17 Hub Locations (Fixed)

South Korea's 17 regional hubs:
- 서울 (Seoul)
- 경기북부 (Gyeonggi North), 경기남부 (Gyeonggi South)
- 인천 (Incheon)
- 강원도 (Gangwon)
- 충청북도 (Chungbuk), 충청남도 (Chungnam)
- 세종 (Sejong), 대전 (Daejeon)
- 전라북도 (Jeonbuk), 전라남도 (Jeonnam)
- 광주 (Gwangju)
- 경상북도 (Gyeongbuk), 경상남도 (Gyeongnam)
- 대구 (Daegu), 부산 (Busan), 울산 (Ulsan)

**Key Constraints**:
- Hub locations are fixed and predefined
- Each hub serves its regional area
- Hub-to-hub routes must be optimized for delivery time

---

## Hub Route Models

The system supports 4 routing strategies (choose one during implementation):

### 1. P2P (Point-to-Point)
**Description**: All hubs directly connected to each other
- **Pros**: Fastest delivery (direct routes)
- **Cons**: High route management complexity (136 routes for 17 hubs)
- **Use Case**: When speed is critical over cost

### 2. Hub-and-Spoke ⭐ RECOMMENDED
**Description**: 3 central hubs act as distribution centers

**Central Hubs**:
- **경기남부 Hub** (5 spokes): 경기북부, 서울, 인천, 경기남부, 강원도
- **대전 Hub** (7 spokes): 충청남도, 충청북도, 세종, 대전, 전라북도, 광주, 전라남도
- **대구 Hub** (5 spokes): 경상북도, 대구, 경상남도, 부산, 울산

**Routing Logic**:
1. Origin hub → Central hub (if not already central)
2. Central hub → Central hub (if different regions)
3. Central hub → Destination hub

**Pros**: Balanced cost and complexity (manageable route count)
**Cons**: Slightly slower than P2P for inter-region deliveries

### 3. P2P + Relay
**Description**: Direct connection for short distances, relay for long distances

**Rules**:
- Direct route if distance < 200km
- Use relay hub if distance ≥ 200km

**Pros**: Optimizes for both speed and cost
**Cons**: Dynamic route calculation required

### 4. Hub-to-Hub Relay
**Description**: Graph-based routing with pathfinding algorithm (Dijkstra's)

**Features**:
- Dynamic optimal path calculation
- Considers traffic, distance, and hub capacity
- Most flexible but computationally expensive

**Pros**: Truly optimal routes
**Cons**: Requires route optimization service, higher complexity

---

## Delivery Personnel Management

### Personnel Types

#### 1. Hub Delivery Staff (10 total)
- **Purpose**: Hub-to-hub transport (long-distance)
- **Assignment**: Not tied to specific hub (`hub_id` is nullable)
- **Count**: 10 staff members (shared across all hubs)
- **Scheduling**: Round-robin assignment

#### 2. Company Delivery Staff (10 per hub)
- **Purpose**: Last-mile delivery (company to customer)
- **Assignment**: Tied to specific hub (`hub_id` required)
- **Count**: 10 staff per hub (170 total for 17 hubs)
- **Scheduling**: Round-robin per hub

### Round-Robin Assignment Algorithm

**Concept**: Sequential assignment using `assign_order` field (0-9)

**Rules**:
1. New personnel registration:
   - Assign `assign_order = max(current_assign_order) + 1`
   - Wrap to 0 when reaching 10
2. Delivery assignment:
   - Find next available staff with `assign_order = (last_assigned_order + 1) % 10`
   - Skip if staff is inactive or already assigned
3. Soft delete:
   - Set `deleted_at`, preserve `assign_order`
   - **Do NOT rearrange** existing orders (maintains fairness)

**Example**:
```
Initial: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
Delete staff 3: [0, 1, 2, X, 4, 5, 6, 7, 8, 9]
Next assignment: Skip 3, assign to 4
Add new staff: Reuse slot 3
```

---

## Order Creation Flow

Critical orchestration logic in **order-service**.

### Step-by-Step Process

#### Phase 1: Validation (Synchronous)
1. **User Authentication**: Verify user is APPROVED and has correct role
2. **Company Validation** (via company-service FeignClient):
   - Supplier company exists and active
   - Receiver company exists and active
3. **Product Validation** (via product-service FeignClient):
   - All products exist and active
   - Check inventory availability (quantity check)

#### Phase 2: Order Creation (Transactional)
4. **Create Order Record**:
   - Generate order_no (format: `ORD-{YYYYMMDD}-{sequence}`)
   - Set status: PENDING
   - Calculate total_amount
5. **Create Order Items**:
   - Line-by-line product details
   - Snapshot product_name and unit_price (immutable)
6. **Deduct Inventory** (via product-service):
   - Reduce stock quantity
   - Handle out-of-stock with compensating transaction

#### Phase 3: Delivery Setup (Asynchronous)
7. **Create Delivery** (via delivery-service FeignClient):
   - Calculate hub route (origin hub → destination hub)
   - Create delivery_route_log entries
   - Assign delivery personnel (round-robin)
8. **Update Order Status**: PENDING → CONFIRMED

#### Phase 4: Notification (Asynchronous)
9. **Calculate Departure Deadline** (via notification-service):
   - AI-based calculation using Gemini API
   - Input: delivery route, customer deadline, work hours (09:00-18:00)
   - Output: Latest departure time from origin hub
10. **Send Slack Notification** (via notification-service):
    - Recipient: Origin hub manager
    - Message: Order details + AI-calculated departure deadline
    - Save notification record (snapshot pattern)

### Error Handling & Rollback

**Scenario 1: Product out of stock (Phase 1)**
- Action: Return error immediately (no order created)
- Response: 400 Bad Request with specific error

**Scenario 2: Inventory deduction fails (Phase 2)**
- Action: Rollback order creation (compensating transaction)
- Response: 500 Internal Server Error

**Scenario 3: Delivery creation fails (Phase 3)**
- Action: Mark order as FAILED, log error, notify admin
- Retry: Manual intervention required

**Scenario 4: Notification fails (Phase 4)**
- Action: Log error, mark notification as FAILED
- Retry: Automatic retry (3 attempts via Resilience4j)

---

## User Registration & Approval Flow

### Step 1: User Registration

**Input Validation**:
- Username: 4-10 chars, alphanumeric lowercase only (e.g., `user123`)
- Password: 8-15 chars, must include special characters (e.g., `Pass@1234`)
- Role: One of [MASTER, HUB_MANAGER, DELIVERY_MANAGER, COMPANY_MANAGER]
- hub_id: Required for HUB_MANAGER and DELIVERY_MANAGER
- company_id: Required for COMPANY_MANAGER

**Initial Status**: PENDING (cannot login yet)

### Step 2: Approval Process

**Who Can Approve**:
- MASTER: Can approve all roles
- HUB_MANAGER: Can approve DELIVERY_MANAGER and COMPANY_MANAGER in their hub

**Approval Actions**:
1. Approver reviews registration request
2. Sets status to APPROVED or REJECTED
3. If approved: User can now login

### Step 3: Authentication

**Login Flow**:
1. User submits username + password
2. System checks:
   - User exists
   - Status = APPROVED (reject if PENDING or REJECTED)
   - Password matches (BCrypt)
3. Generate JWT token:
   - Payload: username, role, hub_id, company_id
   - Expiry: 24 hours (configurable)
4. Return token to client

**Gateway Processing**:
1. Validate JWT signature
2. Extract user info
3. Add headers to downstream services:
   - X-User-Id
   - X-User-Name
   - X-User-Role
   - X-Hub-Id (if applicable)
   - X-Company-Id (if applicable)

---

## Authorization Rules

### Role Hierarchy

| Role | Level | Permissions |
|------|-------|-------------|
| MASTER | 4 (Highest) | Full CRUD on all resources |
| HUB_MANAGER | 3 | CRUD on own hub's resources (companies, products, delivery personnel) |
| DELIVERY_MANAGER | 2 | Update own deliveries, read-only for other resources |
| COMPANY_MANAGER | 1 | CRUD on own company's products, create orders |

### Permission Matrix

| Resource | MASTER | HUB_MANAGER | DELIVERY_MANAGER | COMPANY_MANAGER |
|----------|--------|-------------|------------------|-----------------|
| Users | CRUD | Approve (own hub) | - | - |
| Hubs | CRUD | Read | Read | Read |
| Companies | CRUD | CRUD (own hub) | Read | Read (own) |
| Products | CRUD | CRUD (own hub) | Read | CRUD (own company) |
| Orders | CRUD | Read (own hub) | Read | Create + Read (own company) |
| Deliveries | CRUD | Read (own hub) | Update (assigned to self) | Read (own company) |
| Notifications | Read logs | - | - | - |

### Ownership Validation

**hub_id-based**:
- HUB_MANAGER can only access resources with their hub_id
- Checked via X-Hub-Id header

**company_id-based**:
- COMPANY_MANAGER can only access resources with their company_id
- Checked via X-Company-Id header

**user_id-based**:
- DELIVERY_MANAGER can only update deliveries assigned to them
- Checked via delivery.assigned_staff_id

---

## AI Integration

### Order Notification (Required)

**Trigger**: Order creation (Phase 4)

**AI Task**: Calculate latest departure time

**Input to Gemini API**:
```json
{
  "order_id": "ORD-20251106-001",
  "origin_hub": "경기남부",
  "destination_hub": "부산",
  "route": ["경기남부", "대구", "부산"],
  "customer_deadline": "2025-11-10T18:00:00",
  "work_hours": "09:00-18:00"
}
```

**Prompt Template**:
```
You are a logistics planner. Calculate the latest departure time from the origin hub.

Given:
- Route: {route}
- Customer deadline: {customer_deadline}
- Work hours: {work_hours}
- Estimated travel time per segment: 4 hours

Calculate the latest departure time ensuring:
1. Delivery within work hours
2. Buffer time for delays (1 hour)
3. Respond in ISO 8601 format

Response format: {"departure_deadline": "YYYY-MM-DDTHH:MM:SS"}
```

**Output**: `{"departure_deadline": "2025-11-09T09:00:00"}`

**Slack Message**:
```
🚚 New Order Alert

Order: ORD-20251106-001
Route: 경기남부 → 대구 → 부산
Customer Deadline: 2025-11-10 18:00

⏰ Latest Departure: 2025-11-09 09:00
Please ensure departure by this time!
```

### Daily Route Optimization (Challenge)

**Trigger**: Daily at 06:00 (cron scheduler)

**AI Task**: Optimize delivery order (TSP problem)

**Input to Gemini API**:
```json
{
  "deliveries": [
    {"id": "D1", "location": "37.5665,126.9780"},
    {"id": "D2", "location": "37.5512,126.9882"},
    {"id": "D3", "location": "37.5642,126.9668"}
  ],
  "start_location": "37.5700,126.9800"
}
```

**Output**: Optimized order `["D2", "D1", "D3"]`

**Naver Maps Integration**:
- Call Directions 5 API with waypoints
- Get actual distance and ETA
- Send optimized route to delivery staff via Slack

---

## Key Business Constraints

1. **Work Hours**: 09:00 - 18:00 (no deliveries outside this window)
2. **Hub Capacity**: No limit (assumption for MVP)
3. **Delivery Deadline**: Must be within 7 days of order creation
4. **Inventory**: Real-time stock check (no overselling)
5. **Soft Delete**: All deletes are logical (no physical deletion)
6. **Audit Trail**: All changes tracked (who, when)
7. **Notification Snapshot**: User info frozen at send time (immutable)

---

## Future Enhancements

- Dynamic route calculation based on real-time traffic
- Hub capacity management (max concurrent deliveries)
- Multi-item orders with different delivery dates
- Priority orders (express delivery)
- Delivery time slot selection by customer
- Automated inventory replenishment
