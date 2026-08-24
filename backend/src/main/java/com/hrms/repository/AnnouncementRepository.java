package com.hrms.repository;

import com.hrms.domain.hr.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findTop10ByStatusOrderByPublishTimeDesc(String status);
}
