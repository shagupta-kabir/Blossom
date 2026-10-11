package io.blossom.erp;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BlossomErpApplication {
    public static void main(String[] args) {
        // "Today", leave dates and overdue checks follow Bangladesh time (UTC+6) even though Render runs in UTC.
        TimeZone.setDefault(TimeZone.getTimeZone(System.getenv().getOrDefault("APP_TIMEZONE", "Asia/Dhaka")));
        java.io.File local = new java.io.File("local.yml").getAbsoluteFile();
        System.out.println("[Blossom] working directory: " + local.getParent() + " | local.yml found: " + local.exists());
        SpringApplication.run(BlossomErpApplication.class, args);
    }
}
