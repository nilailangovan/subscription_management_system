# 🚀 LoopPay Subscription Management System — Authentication Module

Spring Boot 3 + PostgreSQL backend with JWT Authentication, Role-Based Access Control (`ADMIN`, `STAFF`, `CUSTOMER`), and password recovery.

Built strictly according to [`RULES.md`](../RULES.md).

---

## 🛠️ Tech Stack & Requirements

- **Brand / Product**: LoopPay
- **Java**: 17
- **Spring Boot**: 3.2.5
- **Persistence**: Spring Data JPA + Hibernate
- **Database**: PostgreSQL (`looppay_db`)
- **Security**: Spring Security 6 + JJWT 0.12.5 (Stateless Bearer Tokens)
- **Validation**: Jakarta Validation (`@Valid`, `@Pattern`, `@Email`, etc.)
- **Mailing**: Spring Mail (Console mode & Mailtrap support)
- **Build Tool**: Maven

---

## 📁 Package Architecture

```
com.brightloop.subscriptionmanagement
├── config
│   ├── CustomAccessDeniedHandler.java   # Returns clean JSON 403 Forbidden
│   ├── DataSeeder.java                  # Seeds default ADMIN account on startup
│   ├── JwtAuthenticationEntryPoint.java # Returns clean JSON 401 Unauthorized
│   ├── JwtAuthenticationFilter.java     # Validates JWT tokens per request
│   ├── JwtUtil.java                     # Generates and parses JWT tokens
│   └── SecurityConfig.java              # Security filter chain & password encoder
├── controller
│   └── AuthController.java              # REST endpoints for authentication & user management
├── dto
│   ├── request
│   │   ├── CreateStaffRequestDto.java   # Admin payload to create STAFF users
│   │   ├── ForgotPasswordRequestDto.java# Payload to request password reset link
│   │   ├── LoginRequestDto.java         # Login credentials payload
│   │   ├── ResetPasswordRequestDto.java # Reset token + new password payload
│   │   └── SignupRequestDto.java        # Customer registration payload
│   └── response
│       ├── ApiResponseDto.java          # Uniform response envelope
│       ├── AuthResponseDto.java         # JWT token + user information
│       ├── ErrorResponseDto.java        # Structured error & validation message
│       └── UserResponseDto.java         # Sanitized user entity data (no password)
├── entity
│   ├── InvoiceStatus.java               # DRAFT, CONFIRMED, PAID
│   ├── PasswordResetToken.java          # Reset token store with 1-hr expiry
│   ├── Role.java                        # ADMIN, STAFF, CUSTOMER
│   ├── SubscriptionStatus.java          # DRAFT, QUOTATION, CONFIRMED, ACTIVE, CLOSED
│   └── User.java                        # User account with BCrypt password & role
├── exception
│   ├── BusinessRuleException.java       # HTTP 400
│   ├── EmailAlreadyExistsException.java # HTTP 409
│   ├── GlobalExceptionHandler.java      # Intercepts exceptions and returns ErrorResponseDto
│   ├── InvalidTokenException.java       # HTTP 400
│   └── ResourceNotFoundException.java   # HTTP 404
├── repository
│   ├── PasswordResetTokenRepository.java# Token lookups & cleanup queries
│   └── UserRepository.java              # User queries by email & existence checks
└── service
    ├── AuthService.java                 # Signup, login, staff creation, password reset logic
    └── EmailService.java                # Reset email sender (console or Mailtrap)
```

---

## ⚙️ Configuration (`src/main/resources/application.properties`)

### 1. Database Setup
Create your database in PostgreSQL (via pgAdmin or psql):
```sql
CREATE DATABASE looppay_db;
```

Credentials in `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/looppay_db
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### 2. Default Seeded Admin
On application startup, `DataSeeder` automatically verifies and seeds the default admin account:
- **Email**: `admin@looppay.com`
- **Password**: `Admin@1234`
- **Role**: `ADMIN`

### 3. Password Reset / Mail Configuration
By default, `app.mail.use-console=true` is enabled. Reset links will print directly to the console:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📧 PASSWORD RESET EMAIL (Console Mode)
   To      : customer@example.com
   Name    : Jane
   Token   : 9b2d8f63-3f6e-4e4b-9721-3a05f1fbe937
   Link    : http://localhost:3000/reset-password?token=9b2d8f63-3f6e-4e4b-9721-3a05f1fbe937
   Expires : 1 hour from now
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

To switch to **Mailtrap**:
1. Change `app.mail.use-console=false`
2. Provide your Mailtrap credentials in `spring.mail.username` and `spring.mail.password`.

---

## 🏃 Running the Application

Using Maven:
```powershell
# Run the application
mvn spring-boot:run

# Or run tests
mvn test
```

The server runs at: `http://localhost:8080`

---

## 🧪 API Endpoints & Testing Guide

### 1. Customer Signup
**`POST /api/auth/signup`** (Public)

*Request:*
```json
{
  "firstName": "Jane",
  "lastName": "Doe",
  "email": "jane.doe@example.com",
  "password": "Customer@1234"
}
```

*Response (201 Created):*
```json
{
  "success": true,
  "message": "Account created successfully. Welcome to LoopPay!",
  "data": {
    "id": 2,
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "jane.doe@example.com",
    "role": "CUSTOMER",
    "active": true,
    "createdAt": "2026-10-31T09:00:00"
  },
  "timestamp": "2026-10-31T09:00:00"
}
```

---

### 2. Login
**`POST /api/auth/login`** (Public)

*Request:*
```json
{
  "email": "admin@looppay.com",
  "password": "Admin@1234"
}
```

*Response (200 OK):*
```json
{
  "success": true,
  "message": "Login successful.",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "userId": 1,
    "email": "admin@looppay.com",
    "fullName": "Super Admin",
    "role": "ADMIN"
  },
  "timestamp": "2026-10-31T09:00:00"
}
```

---

### 3. Create STAFF User (Admin Only)
**`POST /api/admin/users/staff`** (Requires `ADMIN` JWT)

*Headers:*
```
Authorization: Bearer <ADMIN_JWT_TOKEN>
Content-Type: application/json
```

*Request:*
```json
{
  "firstName": "Mark",
  "lastName": "Staff",
  "email": "mark.staff@looppay.com",
  "password": "Staff@1234"
}
```

*Response (201 Created):*
```json
{
  "success": true,
  "message": "Staff user created successfully.",
  "data": {
    "id": 3,
    "firstName": "Mark",
    "lastName": "Staff",
    "email": "mark.staff@looppay.com",
    "role": "STAFF",
    "active": true,
    "createdAt": "2026-10-31T09:00:00"
  },
  "timestamp": "2026-10-31T09:00:00"
}
```

---

### 4. Forgot Password
**`POST /api/auth/forgot-password`** (Public)

*Request:*
```json
{
  "email": "jane.doe@example.com"
}
```

*Response (200 OK):*
```json
{
  "success": true,
  "message": "If an account with that email exists, a password reset link has been sent.",
  "data": null,
  "timestamp": "2026-10-31T09:00:00"
}
```

---

### 5. Reset Password
**`POST /api/auth/reset-password`** (Public)

*Request:*
```json
{
  "token": "9b2d8f63-3f6e-4e4b-9721-3a05f1fbe937",
  "newPassword": "NewStrongP@ss2"
}
```

*Response (200 OK):*
```json
{
  "success": true,
  "message": "Password reset successfully. You can now log in with your new password.",
  "data": null,
  "timestamp": "2026-10-31T09:00:00"
}
```

---

## 🔒 Security & Rules Compliance Summary

| Requirement | Implementation Detail | Status |
|-------------|-----------------------|:------:|
| **DTO Usage** | Every request and response uses dedicated DTOs; Entities are never exposed | ✅ |
| **Password Rule** | Regex: `^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*]).{9,}$` (>8 chars, 1 uppercase, 1 lowercase, 1 special char) | ✅ |
| **Email Uniqueness** | DB Unique constraint on `users(email)` + service check throwing `EmailAlreadyExistsException` (HTTP 409) | ✅ |
| **Password Storage** | BCrypt password hashing (`BCryptPasswordEncoder`) | ✅ |
| **JWT Authentication** | HMAC-SHA signed JWTs with `userId`, `role`, and expiration claims | ✅ |
| **Role Authorization** | `@PreAuthorize("hasRole('ADMIN')")` on staff creation endpoint | ✅ |
| **Default Admin Seed** | Seeded idempotently on startup with credentials in `application.properties` | ✅ |
| **Clean Code & Comments** | Detailed Javadoc and inline explanations suitable for freshers | ✅ |


---

# 📦 Phase 2: Products, Variants & Subscription Plans

LoopPay Phase 2 introduces comprehensive catalog management, variant pricing calculation, subscription plan lifecycles, and automated scheduled auto-closure.

## 🧱 Entities & Business Logic

### 1. Product Entity (`Product`)
- **Fields**: `id`, `name`, `type` (`SERVICE`, `GOODS`, `SOFTWARE`), `salesPrice`, `costPrice`, `notes`, `active` (soft-delete flag), `createdAt`, `updatedAt`.
- **Validation**: `salesPrice >= 0`, `costPrice >= 0`.
- **Relationship**: `@OneToMany` with `ProductVariant`.

### 2. ProductVariant Entity (`ProductVariant`)
- **Fields**: `id`, `product` (parent link), `attribute` (e.g., `"Brand"`, `"Storage"`), `value` (e.g., `"Odoo"`, `"100GB"`), `extraPrice` (e.g., `560.00`), `active`.
- **Pricing Formula**:
  $$\text{Final Price} = \text{Product.salesPrice} + \text{Variant.extraPrice}$$
- **DB Index**: `@Index(name = "idx_product_variants_product_id", columnList = "product_id")`.

### 3. Subscription Plan Entity (`Plan`)
- **Fields**: `name`, `product` (optional base product link), `price`, `billingPeriod` (`DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`), `minQuantity` ($\ge 1$), `startDate`, `endDate`, `autoClose`, `closable`, `pausable`, `renewable`, `status` (`ACTIVE`, `PAUSED`, `CLOSED`), `active`.
- **Validation**:
  - `price >= 0`
  - `minQuantity >= 1`
  - `endDate` strictly after `startDate`
- **DB Indexes**:
  - `@Index(name = "idx_plans_status", columnList = "status")`
  - `@Index(name = "idx_plans_product_id", columnList = "product_id")`
  - `@Index(name = "idx_plans_billing_period", columnList = "billing_period")`

---

## ⚡ Plan Lifecycle Actions (`ADMIN` Only)

All action endpoints enforce business constraints and return HTTP `400 Bad Request` (`BusinessRuleException`) if violated:

| Action | Endpoint | Pre-conditions | Status Transition |
|--------|----------|----------------|-------------------|
| **Close** | `PATCH /api/plans/{id}/close` | `closable == true` and `status != CLOSED` | $\rightarrow$ `CLOSED` |
| **Pause** | `PATCH /api/plans/{id}/pause` | `pausable == true` and `status == ACTIVE` | $\rightarrow$ `PAUSED` |
| **Resume** | `PATCH /api/plans/{id}/resume` | `status == PAUSED` | $\rightarrow$ `ACTIVE` |
| **Renew** | `POST /api/plans/{id}/renew` | `renewable == true` | $\rightarrow$ `ACTIVE`, extends `endDate` |

- **Renewal Logic**:
  - If optional body `{"newEndDate": "YYYY-MM-DD"}` is passed, verifies `newEndDate > current endDate`.
  - If omitted, automatically adds 1 full `billingPeriod` cycle to the current `endDate` (or today if expired).
  - Automatically restores plan status to `ACTIVE`.

---

## ⏰ Automated Scheduled Job (`PlanScheduler`)

LoopPay includes an automated Spring Scheduler (`@EnableScheduling`):
- Runs hourly (`cron = "0 0 * * * *"`)
- Query: Finds all plans where `autoClose == true`, `status != CLOSED`, and `endDate < today`
- Automatically updates status to `CLOSED` and persists changes.

---

## 📡 Phase 2 REST API Reference

### Products Endpoints (`/api/products`)
| Method | Endpoint | RBAC | Description |
|--------|----------|------|-------------|
| `POST` | `/api/products` | `ADMIN` | Create a new product |
| `PUT` | `/api/products/{id}` | `ADMIN` | Update an existing product |
| `DELETE` | `/api/products/{id}` | `ADMIN` | Soft delete a product (`active=false`) |
| `GET` | `/api/products/{id}` | `ADMIN, STAFF` | Get product details by ID |
| `GET` | `/api/products?search={name}&page=0&size=10` | `ADMIN, STAFF` | Paginated search of products |

### Product Variants Endpoints (`/api/products/{productId}/variants` & `/api/products/variants/{variantId}`)
| Method | Endpoint | RBAC | Description |
|--------|----------|------|-------------|
| `POST` | `/api/products/{productId}/variants` | `ADMIN` | Create a variant for a product |
| `PUT` | `/api/products/variants/{variantId}` | `ADMIN` | Update a variant |
| `DELETE` | `/api/products/variants/{variantId}` | `ADMIN` | Soft delete a variant (`active=false`) |
| `GET` | `/api/products/variants/{variantId}` | `ADMIN, STAFF` | Get variant by ID (returns `finalPrice`) |
| `GET` | `/api/products/{productId}/variants` | `ADMIN, STAFF` | Paginated list of variants for a product |

### Plans Endpoints (`/api/plans`)
| Method | Endpoint | RBAC | Description |
|--------|----------|------|-------------|
| `POST` | `/api/plans` | `ADMIN` | Create a new subscription plan |
| `PUT` | `/api/plans/{id}` | `ADMIN` | Update an existing plan |
| `DELETE` | `/api/plans/{id}` | `ADMIN` | Soft delete a plan (`active=false`) |
| `GET` | `/api/plans/{id}` | `ADMIN, STAFF` | Get plan details by ID |
| `GET` | `/api/plans?search={name}&page=0&size=10` | `ADMIN, STAFF` | Paginated search of all plans |
| `GET` | `/api/plans/available?page=0&size=10` | `CUSTOMER, ADMIN, STAFF` | Available plans only (active, `ACTIVE`, valid date range) |
| `PATCH` | `/api/plans/{id}/close` | `ADMIN` | Close plan (enforces `closable`) |
| `PATCH` | `/api/plans/{id}/pause` | `ADMIN` | Pause plan (enforces `pausable`) |
| `PATCH` | `/api/plans/{id}/resume` | `ADMIN` | Resume plan from paused status |
| `POST` | `/api/plans/{id}/renew` | `ADMIN` | Renew plan (enforces `renewable`) |

---

## 🧪 Unit Tests Summary

All Phase 1 and Phase 2 unit tests execute cleanly with **26 passing tests (0 failures, 0 errors)**:

- `AuthServiceTest` (5 tests):
  - User signup validation & password regex verification
  - Duplicate email collision detection (409)
  - Login authentication & JWT generation
  - Bad credentials handling
  - Admin creation of STAFF users
- `ProductVariantServiceTest` (5 tests):
  - Pricing formula: $\text{Final Price} = \text{salesPrice} + \text{extraPrice}$ (e.g. $1000 + 560 = 1560$)
  - Creation of variants with positive/zero extraPrice
  - Rejection of negative extraPrice (400 `BusinessRuleException`)
  - Missing parent product error handling (404 `ResourceNotFoundException`)
  - Soft-deletion validation (`active=false`)
- `PlanServiceTest` (16 tests):
  - Date validation: `endDate` must be strictly after `startDate`
  - Negative price rejection
  - Minimum quantity validation ($\ge 1$)
  - Close action when `closable=true` vs `closable=false` rejection
  - Rejection when closing an already CLOSED plan
  - Pause action when `pausable=true` vs `pausable=false` rejection
  - Rejection when pausing non-ACTIVE plan
  - Resume action when `status=PAUSED` vs `status=ACTIVE` rejection
  - Renew action with explicit `newEndDate`
  - Renew action with billing period cycle extension
  - Renew action rejection when `renewable=false`
  - Scheduled auto-close execution for expired plans with `autoClose=true`

---

# 📋 Phase 3: Customer Subscriptions & Order Management

LoopPay Phase 3 implements the full lifecycle of customer subscriptions, sequential contract numbering, multi-product order lines, strict forward status transitions, and pluggable pricing calculation.

## 🧱 Entities & Architecture

### 1. Subscription Entity (`Subscription`)
- **Fields**: `id`, `subscriptionNumber` (auto-formatted e.g. `SUB-0001`), `customer` (`User`), `plan` (`Plan`), `startDate`, `expiryDate`, `paymentTerms`, `status` (`DRAFT`, `QUOTATION`, `CONFIRMED`, `ACTIVE`, `CLOSED`), `subtotal`, `totalTax`, `totalDiscount`, `grandTotal`, `lines`, `active`, `createdAt`, `updatedAt`.
- **Validation Constraints**:
  - `expiryDate` strictly after `startDate`.
  - Total subscription quantity across lines $\ge$ `Plan.minQuantity`.
- **Database Indexes**:
  - `@Index(name = "idx_subscriptions_customer_id", columnList = "customer_id")`
  - `@Index(name = "idx_subscriptions_status", columnList = "status")`
  - `@Index(name = "idx_subscriptions_plan_id", columnList = "plan_id")`
  - `@Index(name = "idx_subscriptions_number", columnList = "subscription_number")`
  - `@Index(name = "idx_subscriptions_active", columnList = "active")`

### 2. Subscription Line Entity (`SubscriptionLine`)
- **Fields**: `id`, `subscription`, `product`, `quantity` ($\ge 1$), `unitPrice`, `tax`, `discount`, `amount`.
- **Relationship**: `@ManyToOne` with `Subscription` (cascade-all, orphan removal).
- **Database Indexes**:
  - `@Index(name = "idx_subscription_lines_sub_id", columnList = "subscription_id")`
  - `@Index(name = "idx_subscription_lines_product_id", columnList = "product_id")`

---

## ⚡ Strict Lifecycle Transitions (`ADMIN` & `STAFF`)

Subscription status transitions must follow the strict forward path:
$$\text{DRAFT} \longrightarrow \text{QUOTATION} \longrightarrow \text{CONFIRMED} \longrightarrow \text{ACTIVE} \longrightarrow \text{CLOSED}$$

- Any out-of-order jump (e.g. `DRAFT` to `ACTIVE` or `CONFIRMED` to `QUOTATION`) throws HTTP `400 Bad Request` (`BusinessRuleException`).
- Closed subscriptions cannot be transitioned to any other status.
- Updates are strictly restricted to `DRAFT` status.

---

## 💰 Pricing Engine Integration (`PricingService`)

LoopPay delegates line and order pricing through a decoupled `PricingService` interface:
```java
public interface PricingService {
    PricingResult calculate(List<PricingLineRequest> lines);
}
```
- **Stub Implementation**: `PricingServiceStubImpl` provides zero tax and zero discount fallback so the system compiles and operates until the dedicated pricing engine is merged.

---

## 📡 Phase 3 REST API Reference

| Method | Endpoint | RBAC | Description |
|--------|----------|------|-------------|
| `POST` | `/api/subscriptions` | `ADMIN, STAFF` | Create a new subscription in `DRAFT` status |
| `PUT` | `/api/subscriptions/{id}` | `ADMIN, STAFF` | Update subscription details (permitted only in `DRAFT`) |
| `PATCH` | `/api/subscriptions/{id}/status` | `ADMIN, STAFF` | Advance status strictly (`DRAFT -> QUOTATION -> CONFIRMED -> ACTIVE -> CLOSED`) |
| `GET` | `/api/subscriptions/{id}` | `ADMIN, STAFF, CUSTOMER` | Get subscription by ID (CUSTOMER can only view their own) |
| `GET` | `/api/subscriptions?status={status}&search={num}&page=0&size=10` | `ADMIN, STAFF, CUSTOMER` | Paginated list (CUSTOMER view automatically scoped to own account) |
| `POST` | `/api/subscriptions/from-template/{templateId}` | `ADMIN, STAFF` | Quotation Template creation hook (TODO for future phase) |

---

## 🧪 Unit Tests Summary (46 Passing Tests)

All unit tests execute cleanly with **46 passing tests (0 failures, 0 errors)**:
- `AuthServiceTest` (5 tests): Authentication, password rules, role generation.
- `ProductVariantServiceTest` (5 tests): Price calculation formulas and soft deletes.
- `PlanServiceTest` (16 tests): Plan lifecycles, renewal cycles, and auto-close scheduler.
- `SubscriptionServiceTest` (20 tests):
  - Sequential `SUB-0001` auto numbering and creation in `DRAFT`.
  - Date validation (`expiryDate > startDate`).
  - Plan `minQuantity` enforcement.
  - Updates allowed only in `DRAFT` status (rejected in other statuses).
  - Strict step-by-step lifecycle transitions (`DRAFT -> QUOTATION -> CONFIRMED -> ACTIVE -> CLOSED`).
  - Rejection of invalid, backward, or terminal status transitions (HTTP 400).
  - RBAC ownership rules: Customer forbidden from viewing other customers' subscriptions (`AccessDeniedException`).
  - Automatic query scoping for Customer listings.
