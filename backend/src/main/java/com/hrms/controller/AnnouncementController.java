package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.domain.hr.Announcement;
import com.hrms.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementRepository announcementRepository;

    @GetMapping
    public ApiResponse<List<Announcement>> list() {
        return ApiResponse.ok(announcementRepository.findTop10ByStatusOrderByPublishTimeDesc("PUBLISHED"));
    }

    /** 发布公告仅限管理员 / HR；查看对所有登录角色开放 */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ApiResponse<Announcement> create(@RequestBody Announcement announcement) {
        announcement.setStatus("PUBLISHED");
        announcement.setPublishTime(LocalDateTime.now());
        announcement.setCreatedAt(LocalDateTime.now());
        return ApiResponse.ok(announcementRepository.save(announcement));
    }
}
