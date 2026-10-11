package io.blossom.erp.controller;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public liveness endpoint used by Render. Always answers 200 so the service is not killed during a DB blip. */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final MongoTemplate mongo;
    private final boolean demoData;
    private final String demoPassword;
    private final boolean signupEnabled;
    private final boolean signupCodeRequired;

    public HealthController(MongoTemplate mongo,
                            @Value("${app.seed.demo-data:true}") boolean demoData,
                            @Value("${app.seed.demo-password:demo1234}") String demoPassword,
                            @Value("${app.signup.enabled:true}") boolean signupEnabled,
                            @Value("${app.signup.code:}") String signupCode) {
        this.mongo = mongo;
        this.demoData = demoData;
        this.demoPassword = demoPassword;
        this.signupEnabled = signupEnabled;
        this.signupCodeRequired = signupCode != null && !signupCode.isBlank();
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "blossom-erp");
        body.put("time", Instant.now().toString());
        String db;
        try {
            mongo.getDb().runCommand(new Document("ping", 1));
            db = "UP";
        } catch (Exception ex) {
            db = "DOWN";
        }
        body.put("database", db);
        body.put("demoData", demoData);
        if (demoData) {
            body.put("demoPassword", demoPassword);   // demo only: shown on the login page for evaluators
        }
        body.put("signupEnabled", signupEnabled);
        body.put("signupCodeRequired", signupCodeRequired);
        return body;
    }
}
