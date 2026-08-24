package com.hrms.repository;

import com.hrms.domain.system.DictItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DictItemRepository extends JpaRepository<DictItem, Long> {

    List<DictItem> findByTypeAndStatusOrderBySortAsc(String type, Integer status);

    List<DictItem> findByTypeOrderBySortAsc(String type);
}
