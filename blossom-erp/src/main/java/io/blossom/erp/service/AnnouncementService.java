package io.blossom.erp.service;

import io.blossom.erp.domain.Announcement;
import io.blossom.erp.dto.WorkDtos.AnnouncementRequest;
import io.blossom.erp.dto.WorkDtos.AnnouncementResponse;
import io.blossom.erp.exception.ResourceNotFoundException;
import io.blossom.erp.repository.AnnouncementRepository;
import io.blossom.erp.security.AuthUser;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcements;

    public AnnouncementService(AnnouncementRepository announcements) {
        this.announcements = announcements;
    }

    public List<AnnouncementResponse> list() {
        return announcements.findAll().stream()
                .sorted(Comparator.comparing(Announcement::isPinned).reversed()
                        .thenComparing(Announcement::getCreatedAt, Comparator.reverseOrder()))
                .map(AnnouncementService::toResponse).toList();
    }

    public AnnouncementResponse create(AnnouncementRequest req, AuthUser author) {
        Announcement a = new Announcement();
        a.setAuthorName(author.fullName());
        a.setCreatedAt(Instant.now());
        a.setTitle(req.title().trim());
        a.setBody(req.body());
        a.setPinned(req.pinned());
        return toResponse(announcements.save(a));
    }

    public AnnouncementResponse update(String id, AnnouncementRequest req) {
        Announcement a = find(id);
        a.setTitle(req.title().trim());
        a.setBody(req.body());
        a.setPinned(req.pinned());
        return toResponse(announcements.save(a));
    }

    public void delete(String id) {
        announcements.delete(find(id));
    }

    private Announcement find(String id) {
        return announcements.findById(id).orElseThrow(() -> new ResourceNotFoundException("Announcement", id));
    }

    static AnnouncementResponse toResponse(Announcement a) {
        return new AnnouncementResponse(a.getId(), a.getTitle(), a.getBody(), a.isPinned(), a.getAuthorName(), a.getCreatedAt());
    }
}
