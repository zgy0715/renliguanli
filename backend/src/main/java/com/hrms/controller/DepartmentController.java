package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.domain.employee.Employee;
import com.hrms.domain.org.Department;
import com.hrms.repository.DepartmentRepository;
import com.hrms.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    /** 组织架构树（含人数与部门成员） */
    @GetMapping("/tree")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ApiResponse<List<Map<String, Object>>> tree() {
        List<Department> all = departmentRepository.findAllByOrderBySortAsc();
        // 一次性取全部在职（非离职）员工，按部门分组，避免 N+1 查询
        List<Employee> active = employeeRepository.findByStatusNotOrderByHireDateAsc("LEFT");
        Map<Long, List<Employee>> byDept = active.stream()
                .filter(e -> e.getDepartment() != null)
                .collect(Collectors.groupingBy(e -> e.getDepartment().getId()));

        Map<Long, Long> counts = all.stream()
                .collect(Collectors.toMap(Department::getId, d -> (long) byDept.getOrDefault(d.getId(), List.of()).size()));
        // 父部门人数 = 自身 + 子树
        Map<Long, Long> totalCounts = new HashMap<>(counts);
        // 从叶子向上累加
        // 按深度降序：先累加叶子，再逐层向上合并，保证父节点拿到完整子树人数
        List<Department> sorted = all.stream()
                .sorted(Comparator.<Department>comparingInt(d -> d.getParentId() == null ? 0 : depthOf(d, all)).reversed())
                .collect(Collectors.toList());
        for (int i = 0; i < sorted.size(); i++) {
            Department d = sorted.get(i);
            if (d.getParentId() != null) {
                totalCounts.merge(d.getParentId(), totalCounts.getOrDefault(d.getId(), 0L), Long::sum);
            }
        }

        Map<Long, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        Map<String, Object> root = null;
        for (Department d : all) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", d.getId());
            node.put("name", d.getName());
            node.put("code", d.getCode());
            node.put("parentId", d.getParentId());
            node.put("leaderId", d.getLeaderId());
            node.put("count", totalCounts.getOrDefault(d.getId(), 0L));
            node.put("directCount", counts.getOrDefault(d.getId(), 0L));
            node.put("members", byDept.getOrDefault(d.getId(), List.of()).stream()
                    .map(e -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", e.getId());
                        m.put("employeeNo", e.getEmployeeNo());
                        m.put("name", e.getName());
                        m.put("positionName", e.getPosition() == null ? null : e.getPosition().getName());
                        m.put("level", e.getLevel());
                        return m;
                    }).collect(Collectors.toList()));
            node.put("children", new ArrayList<Map<String, Object>>());
            nodeMap.put(d.getId(), node);
            if (d.getParentId() == null) root = node;
        }
        for (Department d : all) {
            Map<String, Object> node = nodeMap.get(d.getId());
            if (d.getParentId() != null && nodeMap.containsKey(d.getParentId())) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) nodeMap.get(d.getParentId()).get("children");
                children.add(node);
            }
        }
        return ApiResponse.ok(root == null ? List.of() : List.of(root));
    }

    private int depthOf(Department d, List<Department> all) {
        int depth = 0;
        Long pid = d.getParentId();
        Map<Long, Department> map = all.stream().collect(Collectors.toMap(Department::getId, x -> x));
        while (pid != null) {
            depth++;
            Department p = map.get(pid);
            if (p == null) break;
            pid = p.getParentId();
        }
        return depth;
    }
}
