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
