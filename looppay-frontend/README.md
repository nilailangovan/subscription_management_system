# ⚡ LoopPay — Frontend Web Console

Modern React + Vite + Tailwind CSS frontend for **LoopPay Subscription Management System**.

Built strictly according to [`RULES.md`](../RULES.md) and the LoopPay design system.

---

## 🎨 Design System Tokens

| Token | Hex | Usage |
|---|---|---|
| **`aubergine`** | `#3A0F34` | Sidebar, navbar, headings, primary text, brand icon container |
| **`marigold`** | `#EAAA00` | Primary buttons, active nav items, brand accent (`Repeat` icon) |
| **`silver`** | `#DDE3E8` | Page canvas background, subtle borders, table striping |
| **`white`** | `#FFFFFF` | Elevated cards on the silver canvas |
| **`success`** | `#2E7D5B` | Active subscriptions, paid invoices |
| **`danger`** | `#B3261E` | Closed subscriptions, validation & API errors |
| **`warning`** | `#EAAA00` | Quotation & pending states |
| **`neutral`** | `#6B7280` | Draft states, timestamps, secondary labels |

> **Contrast Rules:**
> - Text on `marigold` buttons is strictly `aubergine` (`#3A0F34`).
> - Never use `marigold` as plain text on light backgrounds.

---

## 📁 Architecture & File Layout

```
looppay-frontend/
├── src/
│   ├── api/
│   │   └── axios.js               # Axios instance with baseURL http://localhost:8080 & JWT Bearer interceptor
│   ├── context/
│   │   └── AuthContext.jsx        # Auth state (user, role, token, login, logout)
│   ├── components/
│   │   ├── common/
│   │   │   ├── Badge.jsx          # Status and role badge
│   │   │   ├── Button.jsx         # Accessible buttons respecting contrast rules
│   │   │   ├── Card.jsx           # White surface card with rounded-xl
│   │   │   ├── Input.jsx          # Form input with validation states & password toggle
│   │   │   ├── Modal.jsx          # Dialog modal with backdrop
│   │   │   ├── PasswordRequirementList.jsx # Live password checklist
│   │   │   ├── StatCard.jsx       # Dashboard KPI metric card
│   │   │   └── Table.jsx          # Silver-striped table with pagination
│   │   ├── layout/
│   │   │   ├── AdminLayout.jsx    # Aubergine sidebar + top navbar for Admin/Staff
│   │   │   ├── CustomerLayout.jsx # Clean top-nav portal for Customers
│   │   │   ├── AuthLayout.jsx     # Centered card with aubergine left tagline panel
│   │   │   ├── Navbar.jsx         # Responsive top navbar with LoopPay logo
│   │   │   └── Sidebar.jsx        # Aubergine management sidebar
│   │   └── ProtectedRoute.jsx     # Route guard validating login & role
│   ├── pages/
│   │   ├── admin/
│   │   │   ├── AdminDashboard.jsx # StatCards + "Add Staff" form & modal
│   │   │   └── StaffManagement.jsx# Authorized staff list & creation
│   │   ├── auth/
│   │   │   ├── ForgotPassword.jsx # Request recovery token
│   │   │   ├── Login.jsx          # Sign in & role redirect
│   │   │   ├── ResetPassword.jsx  # Submit UUID token & new password
│   │   │   └── Signup.jsx         # Customer self-registration
│   │   ├── customer/
│   │   │   └── CustomerPortal.jsx # Customer home with empty states for Subscriptions & Invoices
│   │   └── Unauthorized.jsx       # 403 Access Denied fallback
│   ├── utils/
│   │   └── validators.js          # Password (>8 chars, upper, lower, special) & email validation
│   ├── App.jsx                    # Route definitions
│   ├── main.jsx                   # React root entry
│   └── index.css                  # Tailwind styles
├── tailwind.config.js             # Theme tokens (aubergine, marigold, silver, status)
├── vite.config.js                 # Vite configuration (port 3000)
└── package.json
```

---

## 🏃 Running the Frontend

In PowerShell or CMD:

```powershell
cd C:\Users\nila0\Desktop\ODOO_HACK26\looppay-frontend
npm install
npm run dev
```

Open browser at: **`http://localhost:3000`**

