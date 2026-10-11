package io.blossom.erp.seed;

import io.blossom.erp.domain.*;
import io.blossom.erp.repository.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Startup data:
 *  - ADMIN_EMAIL + ADMIN_PASSWORD: creates a first administrator if that email does not exist yet.
 *  - SEED_DEMO_DATA=true: loads a demo company based in Bangladesh (names, cities, Tk salaries and budgets)
 *    when there are no employees yet.
 *  - RESET_DEMO_DATA=true (together with SEED_DEMO_DATA): WIPES every collection first, then loads the demo data.
 *    Use it once if old demo data is already in the database, then remove it again.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppUserRepository users;
    private final DepartmentRepository departments;
    private final EmployeeRepository employees;
    private final LeaveRequestRepository leaves;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final AnnouncementRepository announcements;
    private final PasswordEncoder encoder;
    private final io.blossom.erp.service.DemoAccountService demoAccounts;

    private final boolean demo;
    private final boolean reset;
    private final String demoPassword;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminName;

    public DataSeeder(AppUserRepository users, DepartmentRepository departments, EmployeeRepository employees,
                      LeaveRequestRepository leaves, ProjectRepository projects, TaskRepository tasks,
                      AnnouncementRepository announcements, PasswordEncoder encoder,
                      io.blossom.erp.service.DemoAccountService demoAccounts,
                      @Value("${app.seed.demo-data:true}") boolean demo,
                      @Value("${app.seed.reset:false}") boolean reset,
                      @Value("${app.seed.demo-password:demo1234}") String demoPassword,
                      @Value("${app.seed.admin-email:}") String adminEmail,
                      @Value("${app.seed.admin-password:}") String adminPassword,
                      @Value("${app.seed.admin-name:System Administrator}") String adminName) {
        this.users = users;
        this.departments = departments;
        this.employees = employees;
        this.leaves = leaves;
        this.projects = projects;
        this.tasks = tasks;
        this.announcements = announcements;
        this.encoder = encoder;
        this.demoAccounts = demoAccounts;
        this.demo = demo;
        this.reset = reset;
        this.demoPassword = demoPassword;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminName = adminName;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (demo && reset) {
                log.warn("RESET_DEMO_DATA=true: wiping all collections before loading demo data");
                tasks.deleteAll();
                projects.deleteAll();
                leaves.deleteAll();
                announcements.deleteAll();
                employees.deleteAll();
                departments.deleteAll();
                users.deleteAll();
            }
            bootstrapAdmin();
            if (demo && employees.count() == 0) {
                seedDemo();
            }
        } catch (Exception ex) {
            // Never block startup because of seeding; the health endpoint reports DB problems.
            log.error("Data seeding failed: {}", ex.getMessage(), ex);
        }
        // Separate from the block above on purpose: even if demo data seeding failed or was skipped,
        // the four demo logins must exist and work.
        log.info("Demo mode is {}", demo ? "ON" : "OFF (set SEED_DEMO_DATA=true to enable demo logins)");
        if (demo) {
            try {
                demoAccounts.ensureAll();
                log.info("Demo logins ready: admin@/hr@/manager@/employee@blossom.demo (password = demo password setting)");
            } catch (Exception ex) {
                log.error("Could not prepare demo accounts: {}", ex.getMessage(), ex);
            }
        }
    }

    private void bootstrapAdmin() {
        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
            return;
        }
        String email = adminEmail.trim().toLowerCase();
        if (users.existsByEmail(email)) {
            return;
        }
        createUser(email, adminName, Role.ADMIN, null, adminPassword);
        log.info("Created bootstrap administrator {}", email);
    }

    // ------------------------------------------------------------------------------------------
    // Demo company: "Blossom Technologies Ltd.", Dhaka. All money amounts are in Bangladeshi Taka.
    // Working week in Bangladesh is Sunday to Thursday.
    // ------------------------------------------------------------------------------------------
    private void seedDemo() {
        log.info("Seeding Bangladeshi demo data (SEED_DEMO_DATA=true)");
        LocalDate today = LocalDate.now();

        Map<String, Department> dept = new HashMap<>();
        dept("exec", "Executive Office", "Company leadership and operations (Gulshan, Dhaka)", dept);
        dept("eng", "Engineering", "Product development, QA and design (Dhaka tech team + remote hubs)", dept);
        dept("hr", "Human Resources", "Recruitment, payroll coordination and employee welfare", dept);
        dept("sales", "Sales & Marketing", "B2B sales, campaigns and partnerships across Bangladesh", dept);
        dept("fin", "Accounts & Finance", "Accounts, VAT/TAX compliance and payroll", dept);
        dept("support", "Customer Support", "Bangla and English customer care", dept);

        Map<String, Employee> e = new HashMap<>();
        //     key        first, last                email                              title                       dept     mgr        city          phone            Tk/month  yrs  months
        emp(e, dept, "nusrat", "Nusrat Jahan", "Chowdhury", "nusrat.jahan@blossom.demo", "Chief Operating Officer", "exec", null, "Dhaka", "+8801711000101", 280000, 7, 0);
        emp(e, dept, "mahmud", "Mahmudul", "Hasan", "mahmudul.hasan@blossom.demo", "Head of Engineering", "eng", "nusrat", "Dhaka", "+8801712000102", 220000, 5, 0);
        emp(e, dept, "farzana", "Farzana", "Akter", "farzana.akter@blossom.demo", "HR Manager", "hr", "nusrat", "Dhaka", "+8801713000103", 150000, 4, 0);
        emp(e, dept, "tanvir", "Tanvir", "Ahmed", "tanvir.ahmed@blossom.demo", "Senior Software Engineer", "eng", "mahmud", "Dhaka", "+8801714000104", 130000, 3, 0);
        emp(e, dept, "sadia", "Sadia", "Islam", "sadia.islam@blossom.demo", "QA Engineer", "eng", "mahmud", "Chattogram", "+8801715000105", 75000, 1, 2);
        emp(e, dept, "imran", "Imran", "Hossain", "imran.hossain@blossom.demo", "Backend Engineer", "eng", "mahmud", "Sylhet", "+8801716000106", 90000, 2, 0);
        emp(e, dept, "tasnim", "Tasnim", "Rahman", "tasnim.rahman@blossom.demo", "UI/UX Designer", "eng", "mahmud", "Dhaka", "+8801717000107", 80000, 2, 6);
        emp(e, dept, "shakib", "Shakib", "Al Amin", "shakib.alamin@blossom.demo", "Mobile Developer", "eng", "mahmud", "Rajshahi", "+8801818000108", 85000, 1, 6);
        emp(e, dept, "mehedi", "Mehedi", "Hasan", "mehedi.hasan@blossom.demo", "HR Executive", "hr", "farzana", "Dhaka", "+8801919000109", 55000, 1, 0);
        emp(e, dept, "kamrul", "Kamrul", "Islam", "kamrul.islam@blossom.demo", "Head of Sales", "sales", "nusrat", "Dhaka", "+8801720000110", 160000, 5, 0);
        emp(e, dept, "ishrat", "Ishrat", "Jahan", "ishrat.jahan@blossom.demo", "Marketing Executive", "sales", "kamrul", "Chattogram", "+8801721000111", 60000, 1, 8);
        emp(e, dept, "sumon", "Sumon", "Mia", "sumon.mia@blossom.demo", "Sales Executive", "sales", "kamrul", "Khulna", "+8801622000112", 50000, 0, 10);
        emp(e, dept, "shamima", "Shamima", "Nasrin", "shamima.nasrin@blossom.demo", "Chief Accountant", "fin", "nusrat", "Dhaka", "+8801723000113", 140000, 6, 0);
        emp(e, dept, "mamun", "Abdullah Al", "Mamun", "abdullah.mamun@blossom.demo", "Accounts Officer", "fin", "shamima", "Dhaka", "+8801324000114", 65000, 2, 0);
        emp(e, dept, "rumana", "Rumana", "Parvin", "rumana.parvin@blossom.demo", "Customer Support Lead", "support", "nusrat", "Dhaka", "+8801725000115", 70000, 3, 0);
        emp(e, dept, "jahid", "Jahid", "Hasan", "jahid.hasan@blossom.demo", "Support Executive", "support", "rumana", "Barishal", "+8801826000116", 38000, 0, 7);

        head(dept, "exec", e.get("nusrat"));
        head(dept, "eng", e.get("mahmud"));
        head(dept, "hr", e.get("farzana"));
        head(dept, "sales", e.get("kamrul"));
        head(dept, "fin", e.get("shamima"));
        head(dept, "support", e.get("rumana"));

        createUser("admin@blossom.demo", "Nusrat Jahan Chowdhury", Role.ADMIN, e.get("nusrat").getId(), demoPassword);
        createUser("hr@blossom.demo", "Farzana Akter", Role.HR, e.get("farzana").getId(), demoPassword);
        createUser("manager@blossom.demo", "Mahmudul Hasan", Role.MANAGER, e.get("mahmud").getId(), demoPassword);
        createUser("employee@blossom.demo", "Tanvir Ahmed", Role.EMPLOYEE, e.get("tanvir").getId(), demoPassword);
        createUser("demoadmin@blossom.demo", "Demo Administrator", Role.ADMIN, e.get("nusrat").getId(), demoPassword);
        // ---- projects (budgets in Tk) ----
        Project pay = project("bKash & Nagad Payment Integration", "Let customers pay invoices with bKash, Nagad and Rocket alongside cards.",
                ProjectStatus.ACTIVE, e.get("mahmud"), today.minusWeeks(4), today.plusWeeks(8), 2_500_000L);
        Project bangla = project("Bangla Localisation of Customer Portal", "Full Bangla (বাংলা) interface, Bangla numerals and Taka formatting.",
                ProjectStatus.ACTIVE, e.get("tasnim"), today.minusWeeks(2), today.plusWeeks(6), 1_200_000L);
        Project eid = project("Eid Sales Campaign", "Festival offers for retail partners in Dhaka, Chattogram and Sylhet.",
                ProjectStatus.PLANNED, e.get("kamrul"), today.plusWeeks(2), today.plusMonths(4), 800_000L);
        Project payroll = project("Payroll & Provident Fund Automation", "Automate monthly payroll, festival bonus and provident fund statements.",
                ProjectStatus.PLANNED, e.get("shamima"), today.plusWeeks(1), today.plusMonths(5), 1_800_000L);
        Project vat = project("VAT Invoice (Mushak) Module", "Generate Mushak-6.3 style VAT challans from sales orders.",
                ProjectStatus.COMPLETED, e.get("mamun"), today.minusMonths(6), today.minusMonths(1), 600_000L);

        task(pay, "Set up bKash sandbox merchant account", TaskStatus.DONE, Priority.MEDIUM, e.get("tanvir"), today.minusDays(10));
        task(pay, "Implement Nagad payment callback", TaskStatus.IN_PROGRESS, Priority.HIGH, e.get("imran"), today.plusDays(5));
        task(pay, "Reconcile gateway settlements with accounts", TaskStatus.TODO, Priority.MEDIUM, e.get("mamun"), today.plusDays(20));
        task(pay, "Write payment regression test plan", TaskStatus.REVIEW, Priority.URGENT, e.get("sadia"), today.minusDays(1));
        task(bangla, "Add Noto Sans Bengali fonts and fallbacks", TaskStatus.DONE, Priority.LOW, e.get("tasnim"), today.minusDays(6));
        task(bangla, "Translate checkout screens to Bangla", TaskStatus.IN_PROGRESS, Priority.HIGH, e.get("tasnim"), today.plusDays(9));
        task(bangla, "Show Taka amounts with lakh/crore grouping", TaskStatus.TODO, Priority.MEDIUM, e.get("shakib"), today.plusDays(12));
        task(eid, "Prepare retailer offer sheet", TaskStatus.TODO, Priority.MEDIUM, e.get("ishrat"), today.plusWeeks(3));
        task(eid, "Book outdoor banners in Chattogram", TaskStatus.TODO, Priority.LOW, e.get("sumon"), today.plusWeeks(5));
        task(payroll, "Collect festival bonus rules from HR", TaskStatus.TODO, Priority.MEDIUM, e.get("mehedi"), today.plusWeeks(2));
        task(payroll, "Evaluate payroll vendors", TaskStatus.TODO, Priority.MEDIUM, null, today.plusWeeks(3));
        task(vat, "Mushak template sign-off by accounts", TaskStatus.DONE, Priority.HIGH, e.get("shamima"), today.minusMonths(1));

        // ---- leave (Bangladesh Labour Act style types) ----
        leave(e.get("tanvir"), LeaveType.CASUAL, today.plusDays(14), today.plusDays(15), "Family function in Cumilla", LeaveStatus.PENDING, null);
        leave(e.get("sadia"), LeaveType.SICK, today.minusDays(1), today.plusDays(1), "Dengue fever, doctor's advice attached", LeaveStatus.APPROVED, "Mahmudul Hasan");
        leave(e.get("sumon"), LeaveType.ANNUAL, today.plusDays(30), today.plusDays(36), "Visiting home village in Khulna", LeaveStatus.PENDING, null);
        leave(e.get("ishrat"), LeaveType.CASUAL, today.minusDays(12), today.minusDays(12), "Personal work", LeaveStatus.REJECTED, "Kamrul Islam");

        // ---- announcements ----
        announce("Welcome to Blossom ERP", "Assalamu Alaikum! This is our new home for people, projects and company news. Office hours are Sunday to Thursday, 9:00 AM to 6:00 PM (Dhaka time).", true, "Nusrat Jahan Chowdhury");
        announce("Eid holiday and festival bonus", "The office will follow the government holiday schedule for Eid. Festival bonus will be credited to salary accounts before the holidays begin. Please submit pending leave requests early.", true, "Farzana Akter");
        announce("Pohela Boishakh celebration", "Join us on 14 April for Boishakhi lunch (panta-ilish!) and cultural programme at the Gulshan office. Wear traditional dress if you like. Shubho Noboborsho!", false, "Farzana Akter");
        announce("Victory Day closure", "The office will be closed on 16 December for Victory Day. Support rota for urgent tickets will be shared by Rumana Parvin.", false, "Nusrat Jahan Chowdhury");
        announce("Salary disbursement", "Monthly salaries are credited by the last working day of each month. Questions about payslips, tax (TIN) or provident fund can be sent to accounts.", false, "Shamima Nasrin");
        log.info("Demo data ready. Demo accounts: admin@/hr@/manager@/employee@blossom.demo");
    }

    private void dept(String key, String name, String desc, Map<String, Department> out) {
        Department d = new Department();
        d.setName(name);
        d.setDescription(desc);
        d.setCreatedAt(Instant.now());
        out.put(key, departments.save(d));
    }

    private void head(Map<String, Department> depts, String key, Employee head) {
        Department d = depts.get(key);
        d.setHeadEmployeeId(head.getId());
        departments.save(d);
    }

    private void emp(Map<String, Employee> out, Map<String, Department> depts, String key, String first, String last,
                     String email, String title, String deptKey, String managerKey, String city, String phone,
                     long monthlySalaryTk, int yearsAgo, int monthsAgo) {
        Employee e = new Employee();
        e.setFirstName(first);
        e.setLastName(last);
        e.setEmail(email);
        e.setPhone(phone);
        e.setJobTitle(title);
        e.setDepartmentId(depts.get(deptKey).getId());
        e.setManagerId(managerKey == null ? null : out.get(managerKey).getId());
        e.setCity(city);
        e.setMonthlySalary(monthlySalaryTk);
        e.setHireDate(LocalDate.now().minusYears(yearsAgo).minusMonths(monthsAgo));
        e.setStatus(EmployeeStatus.ACTIVE);
        e.setCreatedAt(Instant.now());
        out.put(key, employees.save(e));
    }

    private void createUser(String email, String name, Role role, String employeeId, String rawPassword) {
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setFullName(name);
        u.setRole(role);
        u.setEmployeeId(employeeId);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setActive(true);
        u.setCreatedAt(Instant.now());
        users.save(u);
    }

    private Project project(String name, String desc, ProjectStatus status, Employee owner, LocalDate start, LocalDate due, long budgetTk) {
        Project p = new Project();
        p.setName(name);
        p.setDescription(desc);
        p.setStatus(status);
        p.setOwnerId(owner.getId());
        p.setStartDate(start);
        p.setDueDate(due);
        p.setBudget(budgetTk);
        p.setCreatedAt(Instant.now());
        return projects.save(p);
    }

    private void task(Project project, String title, TaskStatus status, Priority priority, Employee assignee, LocalDate due) {
        Task t = new Task();
        t.setProjectId(project.getId());
        t.setTitle(title);
        t.setStatus(status);
        t.setPriority(priority);
        t.setAssigneeId(assignee == null ? null : assignee.getId());
        t.setDueDate(due);
        t.setCreatedAt(Instant.now());
        tasks.save(t);
    }

    private void leave(Employee emp, LeaveType type, LocalDate from, LocalDate to, String reason, LeaveStatus status, String reviewer) {
        LeaveRequest l = new LeaveRequest();
        l.setEmployeeId(emp.getId());
        l.setType(type);
        l.setStartDate(from);
        l.setEndDate(to);
        l.setDays((int) ChronoUnit.DAYS.between(from, to) + 1);
        l.setReason(reason);
        l.setStatus(status);
        l.setCreatedAt(Instant.now());
        if (status != LeaveStatus.PENDING) {
            l.setReviewerName(reviewer);
            l.setDecidedAt(Instant.now());
        }
        leaves.save(l);
    }

    private void announce(String title, String body, boolean pinned, String author) {
        Announcement a = new Announcement();
        a.setTitle(title);
        a.setBody(body);
        a.setPinned(pinned);
        a.setAuthorName(author);
        a.setCreatedAt(Instant.now());
        announcements.save(a);
    }
}
