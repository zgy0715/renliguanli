package com.hrms.service;

import com.hrms.common.BizException;
import com.hrms.common.PageResult;
import com.hrms.domain.employee.Contract;
import com.hrms.domain.employee.Education;
import com.hrms.domain.employee.Employee;
import com.hrms.domain.employee.Transfer;
import com.hrms.dto.EmployeeVO;
import com.hrms.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final ContractRepository contractRepository;
    private final EducationRepository educationRepository;
    private final TransferRepository transferRepository;
    private final PositionRepository positionRepository;

    @Transactional(readOnly = true)
    public PageResult<EmployeeVO> search(String keyword, Long deptId, String status,
                                         String level, String education, String gender, String tag,
                                         int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        Set<Long> deptIds = collectDeptIds(deptId);
        Specification<Employee> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String k = "%" + keyword + "%";
                ps.add(cb.or(cb.like(root.get("name"), k),
                        cb.like(root.get("employeeNo"), k),
                        cb.like(root.get("phone"), k)));
            }
            if (deptId != null && !deptIds.isEmpty()) {
                ps.add(root.get("department").get("id").in(deptIds));
            }
            if (StringUtils.hasText(status)) {
                ps.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(level)) {
                ps.add(cb.equal(root.get("level"), level));
            }
            if (StringUtils.hasText(education)) {
                ps.add(cb.equal(root.get("education"), education));
            }
            if (StringUtils.hasText(gender)) {
                ps.add(cb.equal(root.get("gender"), gender));
            }
            if (StringUtils.hasText(tag)) {
                ps.add(cb.like(root.get("tags"), "%" + tag + "%"));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Employee> result = employeeRepository.findAll(spec, pageable);
        Map<Long, String> empNameMap = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getId, Employee::getName, (a, b) -> a));
        List<EmployeeVO> vos = result.getContent().stream()
                .map(e -> toVO(e, empNameMap))
                .collect(Collectors.toList());
        return new PageResult<>(result.getTotalElements(), vos);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id) {
        Employee e = employeeRepository.findById(id)
                .orElseThrow(() -> new BizException("员工不存在"));
        Map<String, Object> result = new LinkedHashMap<>();
        // 返回扁平字段（departmentName/positionName/managerName），避免前端拿原生实体渲染出 undefined
        Map<String, Object> emp = new LinkedHashMap<>();
        emp.put("id", e.getId());
        emp.put("employeeNo", e.getEmployeeNo());
        emp.put("name", e.getName());
        emp.put("gender", e.getGender());
        emp.put("birthDate", e.getBirthDate());
        emp.put("phone", e.getPhone());
        emp.put("email", e.getEmail());
        emp.put("departmentName", e.getDepartment() != null ? e.getDepartment().getName() : "-");
        emp.put("positionName", e.getPosition() != null ? e.getPosition().getName() : "-");
        emp.put("level", e.getLevel());
        emp.put("hireDate", e.getHireDate());
        emp.put("regularDate", e.getRegularDate());
        emp.put("status", e.getStatus());
        emp.put("education", e.getEducation());
        emp.put("school", e.getSchool());
        emp.put("major", e.getMajor());
        emp.put("baseSalary", e.getBaseSalary());
        emp.put("tags", e.getTags());
        emp.put("gridCell", e.getGridCell());
        String managerName = null;
        if (e.getManagerId() != null) {
            Employee m = employeeRepository.findById(e.getManagerId()).orElse(null);
            if (m != null) managerName = m.getName();
        }
        emp.put("managerId", e.getManagerId());
        emp.put("managerName", managerName);
        result.put("employee", emp);
        result.put("contracts", contractRepository.findByEmployeeIdOrderByIdAsc(id));
        result.put("education", educationRepository.findByEmployeeIdOrderByStartDateAsc(id));
        result.put("transfers", transferRepository.findByEmployeeIdOrderByEffectiveDateAsc(id));
        result.put("managerName", managerName);
        return result;
    }

    @Transactional
    public Employee create(Employee employee) {
        // 工号唯一校验
        if (!StringUtils.hasText(employee.getEmployeeNo())) {
            throw new BizException("工号不能为空");
        }
        if (employeeRepository.existsByEmployeeNo(employee.getEmployeeNo())) {
            throw new BizException("工号已存在：" + employee.getEmployeeNo());
        }
        employee.setCreatedAt(LocalDateTime.now());
        employee.setUpdatedAt(LocalDateTime.now());
        if (employee.getStatus() == null) employee.setStatus("ACTIVE");
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee update(Long id, Employee input) {
        Employee e = employeeRepository.findById(id)
                .orElseThrow(() -> new BizException("员工不存在"));
        copyNonNull(input, e);
        e.setUpdatedAt(LocalDateTime.now());
        return employeeRepository.save(e);
    }

    @Transactional
    public void delete(Long id) {
        // 级联清理子表（合同/教育/异动为独立表，无外键级联）
        contractRepository.deleteByEmployeeId(id);
        educationRepository.deleteByEmployeeId(id);
        transferRepository.deleteByEmployeeId(id);
        // 清除其他员工对该上级的引用
        employeeRepository.findAll().stream()
                .filter(emp -> id.equals(emp.getManagerId()))
                .forEach(emp -> emp.setManagerId(null));
        // 清除部门负责人引用
        departmentRepository.findAll().stream()
                .filter(d -> id.equals(d.getLeaderId()))
                .forEach(d -> d.setLeaderId(null));
        employeeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<String> allTags() {
        return employeeRepository.findAll().stream()
                .map(Employee::getTags)
                .filter(StringUtils::hasText)
                .flatMap(t -> Arrays.stream(t.split(",")))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private Set<Long> collectDeptIds(Long deptId) {
        if (deptId == null) return Collections.emptySet();
        Set<Long> ids = new HashSet<>();
        ids.add(deptId);
        boolean added = true;
        while (added) {
            added = false;
            List<Long> newIds = new ArrayList<>();
            for (Long pid : ids) {
                for (var d : departmentRepository.findByParentId(pid)) {
                    if (ids.add(d.getId())) {
                        newIds.add(d.getId());
                        added = true;
                    }
                }
            }
            ids.addAll(newIds);
        }
        return ids;
    }

    private EmployeeVO toVO(Employee e, Map<Long, String> empNameMap) {
        return new EmployeeVO(
                e.getId(), e.getEmployeeNo(), e.getName(), e.getGender(), e.getBirthDate(),
                e.getPhone(), e.getEmail(),
                e.getDepartment() != null ? e.getDepartment().getId() : null,
                e.getDepartment() != null ? e.getDepartment().getName() : null,
                e.getPosition() != null ? e.getPosition().getId() : null,
                e.getPosition() != null ? e.getPosition().getName() : null,
                e.getLevel(), e.getHireDate(), e.getRegularDate(), e.getStatus(), e.getEducation(),
                e.getBaseSalary(), e.getTags(), e.getGridCell(),
                e.getManagerId(), e.getManagerId() != null ? empNameMap.get(e.getManagerId()) : null,
                e.getProfileScore(), e.getAvatar());
    }

    private void copyNonNull(Employee src, Employee dst) {
        if (src.getEmployeeNo() != null && !src.getEmployeeNo().equals(dst.getEmployeeNo())) {
            if (employeeRepository.existsByEmployeeNo(src.getEmployeeNo())) {
                throw new BizException("工号已存在：" + src.getEmployeeNo());
            }
            dst.setEmployeeNo(src.getEmployeeNo());
        }
        if (src.getName() != null) dst.setName(src.getName());
        if (src.getGender() != null) dst.setGender(src.getGender());
        if (src.getPhone() != null) dst.setPhone(src.getPhone());
        if (src.getEmail() != null) dst.setEmail(src.getEmail());
        if (src.getStatus() != null) dst.setStatus(src.getStatus());
        if (src.getLevel() != null) dst.setLevel(src.getLevel());
        if (src.getTags() != null) dst.setTags(src.getTags());
        if (src.getGridCell() != null) dst.setGridCell(src.getGridCell());
        if (src.getEducation() != null) dst.setEducation(src.getEducation());
        if (src.getDepartment() != null) dst.setDepartment(src.getDepartment());
        if (src.getPosition() != null) dst.setPosition(src.getPosition());
        if (src.getManagerId() != null) dst.setManagerId(src.getManagerId());
        if (src.getBaseSalary() != null) dst.setBaseSalary(src.getBaseSalary());
        if (src.getHireDate() != null) dst.setHireDate(src.getHireDate());
        if (src.getLeaveDate() != null) dst.setLeaveDate(src.getLeaveDate());
    }
}
