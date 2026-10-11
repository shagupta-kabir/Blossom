package io.blossom.erp.repository;

import io.blossom.erp.domain.AppUser;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AppUserRepository extends MongoRepository<AppUser, String> {
    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);
}
