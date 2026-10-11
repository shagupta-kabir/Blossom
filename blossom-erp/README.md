# 🌸 Blossom ERP

A compact ERP backend (plus a mobile-friendly web UI) for **employees, departments, leave, projects & tasks, announcements and dashboard statistics**, with JWT authentication and role-based access control.

🇧🇩 **Built for Bangladesh:** all money is in **Bangladeshi Taka (Tk / BDT)** shown with lakh/crore grouping (`Tk 25,00,000`), phone numbers are validated as Bangladeshi mobiles (`+8801XXXXXXXXX` or `01XXXXXXXXX`), dates use Dhaka time (`Asia/Dhaka`), leave types follow the Labour Act vocabulary (casual, sick, annual, maternity), and the demo dataset is a Dhaka-based company with Bangladeshi staff, cities, salaries and holidays.

**Stack:** Java 25 (Amazon Corretto) · Spring Boot 3.5.6 · Spring Security + JWT (jjwt 0.12.6) · Spring Data MongoDB · MongoDB Atlas · Maven · Docker · Render

The web UI (served at `/`) uses a clean, modern SaaS look: white surfaces on a soft slate background, a blue brand colour, Inter typography, inline SVG icons (no emoji, no icon-font dependency), a sidebar layout on desktop and a bottom tab bar on phones.
Brand tokens live at the top of the `<style>` block in `index.html`: `#2563EB` blue (primary) · `#1D4ED8` blue-600 (hover) · `#EFF6FF` blue-50 (active/tint) · `#0F172A` ink · `#64748B` muted · `#E2E8F0` line · `#F8FAFC` background.

---

## For evaluators (faculty): how to check every role

Open the site and use the **Demo accounts** panel on the sign-in page (tap a row to fill the form). Password for all demo accounts: **`demo1234`**.

| Role | Email | Password | What to check |
|---|---|---|---|
| Admin | `admin@blossom.demo` | `demo1234` | everything: **Users** tab, salaries and payroll in Tk, all CRUD |
| HR | `hr@blossom.demo` | `demo1234` | employees + salaries, departments, announcements, decides any leave |
| Manager | `manager@blossom.demo` | `demo1234` | projects/tasks, decides leave only for direct reports, no salaries |
| Employee | `employee@blossom.demo` | `demo1234` | read-only company data, request leave, move own tasks only |

**Sign up:** the *Sign up* tab creates a new **Employee** account (plus an employee profile) and signs the user in. Roles can only be raised by an admin on the **Users** tab. Sign-up can be switched off or put behind admin approval or a code (see environment variables).

---

## 1. Quick start (no laptop needed → see `docs/PHONE_DEPLOY.md`)

### Local with Docker (includes its own MongoDB)
```bash
docker compose up --build
# open http://localhost:8080  → sign in with a demo account
```

### Easiest local run (IntelliJ or Maven) with Atlas
1. Copy `local.yml.example` to `local.yml` (same folder as `pom.xml`).
2. Put your Atlas username and password into the `uri:` line.
3. Run `BlossomErpApplication` (or `./mvnw spring-boot:run`) and open http://localhost:8080.

### Local with Maven using environment variables (needs JDK 25 + a MongoDB)
```bash
export MONGODB_URI="mongodb://localhost:27017/blossom_erp"
export JWT_SECRET="any-random-string-of-at-least-32-characters"   # optional locally
export SEED_DEMO_DATA=true
./mvnw spring-boot:run
```

### Run the tests
```bash
./mvnw verify        # tests need no database (repositories are mocked)
```

---

## 2. Environment variables

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `MONGODB_URI` | **yes** (prod) | `mongodb://localhost:27017/blossom_erp` | MongoDB Atlas connection string (must include the db name) |
| `MONGODB_DATABASE` | no | `blossom_erp` | Database name; used when your URI has no `/name` part |
| `JWT_SECRET` | strongly recommended | random temporary key (logins reset on restart) | HMAC signing key, **≥ 32 characters**; a non-empty value shorter than 32 stops the app |
| `JWT_EXPIRATION_MINUTES` | no | `480` | Token lifetime |
| `SEED_DEMO_DATA` | no | `false` | `true` loads the Bangladeshi demo company + 4 demo accounts on first start (only if no employees exist) |
| `RESET_DEMO_DATA` | no | `false` | ⚠️ with `SEED_DEMO_DATA=true`: **wipes every collection** then reloads the demo data. Use once (e.g. if older demo data is already in Atlas), then remove it |
| `APP_TIMEZONE` | no | `Asia/Dhaka` | Time zone used for "today", leave and overdue checks |
| `DEMO_PASSWORD` | no | `demo1234` | Password of the demo accounts |
| `SIGNUP_ENABLED` | no | `true` | Show/allow the public *Sign up* tab (always creates EMPLOYEE accounts) |
| `SIGNUP_AUTO_APPROVE` | no | `true` | `false` = new accounts stay disabled until an admin enables them on the Users tab |
| `SIGNUP_CODE` | no | – | If set, people must enter this code to sign up |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` / `ADMIN_NAME` | no | – | Creates a first real administrator at startup if that email does not exist |
| `CORS_ALLOWED_ORIGINS` | no | `*` | Comma separated origins for browsers on another domain |
| `PORT` | no | `8080` | Render sets this automatically |

No secret is stored in the repository; everything comes from the environment.

---

## 3. Deploy: MongoDB Atlas + Render

### A. MongoDB Atlas (free)
1. Create an account at <https://cloud.mongodb.com> → **Create a cluster** (free **M0**).
2. **Database Access** → *Add new database user* (username + strong password; avoid `@ : / ?` in the password or URL-encode it).
3. **Network Access** → *Add IP address* → **Allow access from anywhere (`0.0.0.0/0`)**. Render's free tier has no fixed IP, so this is required.
4. **Database → Connect → Drivers** → copy the string and add the database name before the `?`:
   `mongodb+srv://USER:PASSWORD@cluster0.xxxxx.mongodb.net/blossom_erp?retryWrites=true&w=majority`

### B. Render
1. Put this project in a GitHub repository (repo root = the folder containing `pom.xml`).
2. Render dashboard → **New + → Blueprint** → select the repo. Render reads `render.yaml` (Docker runtime, health check `/api/health`, auto-generated `JWT_SECRET`).
3. When asked, paste your Atlas string into **`MONGODB_URI`**. Click **Apply**.
4. First build takes a few minutes. Open the `https://<name>.onrender.com` URL → sign in.

*Manual alternative:* New + → Web Service → your repo → Runtime **Docker** → add `MONGODB_URI`, `JWT_SECRET` (32+ chars), optionally `SEED_DEMO_DATA=true` → Health Check Path `/api/health`.

> Free Render services sleep after ~15 min idle; the first request afterwards can take ~30–60 s.

### C. Going "real" (after trying the demo)
Set `SEED_DEMO_DATA=false`, set `ADMIN_EMAIL` + `ADMIN_PASSWORD` (creates your own admin), and change/remove the demo users via the **Users** tab. Delete the demo users because their password is public in this README.

---

## 4. Demo dataset (Bangladesh, amounts in Tk)

With `SEED_DEMO_DATA=true` the first start creates **Blossom Technologies Ltd.** (Gulshan, Dhaka):

- **6 departments:** Executive Office, Engineering, Human Resources, Sales & Marketing, Accounts & Finance, Customer Support
- **16 employees** with Bangladeshi names, home cities (Dhaka, Chattogram, Sylhet, Rajshahi, Khulna, Barishal), `+8801…` mobiles and monthly salaries from **Tk 38,000 to Tk 2,80,000**
- **5 projects with budgets in Tk** (bKash & Nagad payment integration, Bangla localisation, Eid sales campaign, payroll & provident fund automation, a completed Mushak VAT module) and 12 tasks
- **4 leave requests** (casual, sick, annual) in different states
- **5 announcements** (Eid holiday & festival bonus, Pohela Boishakh, Victory Day, salary disbursement, welcome)

Demo accounts (password = `DEMO_PASSWORD`, default `demo1234`):

| Email | Role | Can do |
|---|---|---|
| `admin@blossom.demo` | ADMIN (Nusrat Jahan Chowdhury, COO) | everything, incl. user accounts and salaries |
| `hr@blossom.demo` | HR (Farzana Akter) | employees & salaries, departments, announcements, decide any leave |
| `manager@blossom.demo` | MANAGER (Mahmudul Hasan, Head of Engineering) | projects/tasks, decide leave of **direct reports only** |
| `employee@blossom.demo` | EMPLOYEE (Tanvir Ahmed) | read data (no salaries), request leave, update **own** tasks |

---

## 5. Roles & permissions

| Area | ADMIN | HR | MANAGER | EMPLOYEE |
|---|:-:|:-:|:-:|:-:|
| View employees, departments, projects, tasks, announcements, dashboard | ✅ | ✅ | ✅ | ✅ |
| Create/edit/delete employees & departments | ✅ | ✅ | – | – |
| Create/edit/delete announcements | ✅ | ✅ | – | – |
| Create/edit/delete projects & tasks | ✅ | – | ✅ | – |
| Move a task's status | ✅ any | – | ✅ any | only tasks assigned to them |
| Request / cancel own leave | ✅ | ✅ | ✅ | ✅ |
| See & decide leave requests | all | all | direct reports | – |
| Manage user accounts | ✅ | – | – | – |
| See employee **salaries** and total **monthly payroll** (Tk) | ✅ | ✅ | – | – |

Money fields (`monthlySalary` on employees, `budget` on projects) are whole Taka (`Long`). `phone` must match `+8801[3-9]XXXXXXXX` or `01[3-9]XXXXXXXX`. Salary is returned as `null` to callers who are not ADMIN/HR.

Rules enforced in the services: nobody can decide their own leave request; overlapping leave is rejected; a department with employees or an employee who manages others cannot be deleted.

---

## 6. API overview

All `/api/**` endpoints except `/api/health` and `/api/auth/login` need `Authorization: Bearer <token>`. Errors share one JSON shape: `{timestamp, status, error, message, path, fieldErrors?}`.

| Method & path | Notes |
|---|---|
| `GET /api/health` | public; `{status, database, ...}` (always HTTP 200) |
| `GET /actuator/health` | public Spring Actuator health |
| `POST /api/auth/login` | `{email, password}` → `{token, user, expiresInSeconds}`; 403 if the account is not approved/disabled |
| `POST /api/auth/register` | public sign-up `{firstName, lastName, email, password, signupCode?}` → `{message, active, auth?}`; always role EMPLOYEE |
| `GET /api/auth/me` · `POST /api/auth/change-password` | current user |
| `GET/POST /api/users` · `PUT /api/users/{id}` | ADMIN |
| `GET /api/employees?q=&departmentId=&status=&page=&size=` · `GET /{id}` | paged search |
| `POST /api/employees` · `PUT/DELETE /api/employees/{id}` | ADMIN, HR |
| `GET /api/departments` · `GET /{id}` · `POST` · `PUT/DELETE /{id}` | writes: ADMIN, HR |
| `POST /api/leave` · `GET /api/leave/mine` · `POST /api/leave/{id}/cancel` | any user with a linked employee profile |
| `GET /api/leave?status=` · `PATCH /api/leave/{id}/decision` | ADMIN, HR, MANAGER |
| `GET/POST /api/projects` · `GET/PUT/DELETE /api/projects/{id}` | writes: ADMIN, MANAGER |
| `GET /api/tasks?projectId=&assigneeId=&status=&mine=true` · `GET /{id}` | |
| `POST /api/tasks` · `PUT/DELETE /api/tasks/{id}` | ADMIN, MANAGER |
| `PATCH /api/tasks/{id}/status` | `{status}`; ADMIN/MANAGER or the assignee |
| `GET /api/announcements` · `POST` · `PUT/DELETE /{id}` | writes: ADMIN, HR |
| `GET /api/dashboard` | headcount, leave, project/task statistics, `currency: "BDT"`, open project budgets (Tk), `monthlyPayroll` (ADMIN/HR only, otherwise null), latest news |

Example:
```bash
TOKEN=$(curl -s -X POST $URL/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@blossom.demo","password":"demo1234"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')
curl -s $URL/api/dashboard -H "Authorization: Bearer $TOKEN"
```

---

## 7. Architecture

```
controller/   REST endpoints + @PreAuthorize role rules
service/      business rules (validation of references, leave rules, permissions)
repository/   Spring Data MongoDB interfaces
domain/       Mongo documents + enums
dto/          request/response records with Bean Validation
security/     JwtService, JwtAuthFilter (stateless), AuthUser principal
config/       SecurityConfig (CORS, filter chain), MongoConfig
exception/    GlobalExceptionHandler → consistent JSON errors
seed/         DataSeeder (bootstrap admin + optional demo data)
static/       index.html – the web UI (vanilla JS, no build step)
```

Design notes: stateless JWT (role carried in the token, no DB hit per request); records for DTOs and no Lombok (fewer moving parts on new JDKs); unique indexes on user/employee email; the `/api/health` endpoint never fails when Atlas is down so Render does not restart-loop you, it reports `database: DOWN` instead.

## 8. Tests

`./mvnw verify` runs:
- `AuthServiceTest` – sign-up (EMPLOYEE only, duplicates, approval, code, disabled) and login rules
- `JwtServiceTest` – signing, tampering, expiry, secret validation
- `LeaveServiceTest` – day counting, overlap rule, manager/HR/self-approval rules, cancellation
- `TaskServiceTest`, `EmployeeServiceTest`, `DashboardServiceTest` – business rules with mocked repositories (incl. Tk salary privacy and payroll totals)
- `RoleBasedAccessTest` – real security chain via MockMvc: 401 without/with bad token, 403 for the wrong role, 400 validation errors (incl. Bangladeshi phone format and negative salary), 404 JSON

CI (`.github/workflows/ci.yml`) runs the same on every push using Corretto 25. The Docker build skips tests so a deploy never fails because of the test environment.

## 9. Credit

Inspired by the feature scope of the uploaded *Enterprise Resource Planning Spring Boot Application* (HR, tasks, announcements-style collaboration, role-based access). This project is an independent implementation: different domain model (MongoDB documents), different module split, JWT-only stateless security, new leave/project workflows.
