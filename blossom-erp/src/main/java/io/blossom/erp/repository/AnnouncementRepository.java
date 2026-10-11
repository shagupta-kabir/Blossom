package io.blossom.erp.repository;

import io.blossom.erp.domain.Announcement;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AnnouncementRepository extends MongoRepository<Announcement, String> {
    List<Announcement> findTop5ByOrderByPinnedDescCreatedAtDesc();
}
