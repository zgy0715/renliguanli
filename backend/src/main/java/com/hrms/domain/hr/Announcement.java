package com.hrms.domain.hr;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业公告
 */
@Data
@Entity
@Table(name = "hr_announcement")
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 2000)
    private String content;

    /** 类型：公告/通知/制度/活动 */
    @Column(length = 16)
    private String type;

    /** 发布人 */
    private Long publisherId;

    /** 状态：DRAFT/已发布/下线 */
    @Column(length = 16)
    private String status;

    /** 是否置顶 */
    private Boolean pinned = false;

    private LocalDateTime publishTime;
    private LocalDateTime createdAt;
}
