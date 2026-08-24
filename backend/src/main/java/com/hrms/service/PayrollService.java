package com.hrms.service;

import com.hrms.common.BizException;
import com.hrms.domain.employee.Employee;
import com.hrms.domain.payroll.PayrollRecord;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.PayrollRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final PayrollRecordRepository payrollRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> list(String month) {
        List<PayrollRecord> records = payrollRepository.findByPayMonthOrderByEmployeeIdAsc(month);
        Map<Long, Employee> empMap = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PayrollRecord r : records) {
            Employee e = empMap.get(r.getEmployeeId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", r.getId());
            row.put("employeeId", r.getEmployeeId());
            row.put("employeeNo", e != null ? e.getEmployeeNo() : "-");
            row.put("name", e != null ? e.getName() : "-");
            row.put("department", e != null && e.getDepartment() != null ? e.getDepartment().getName() : "-");
            row.put("position", e != null && e.getPosition() != null ? e.getPosition().getName() : "-");
            row.put("baseSalary", r.getBaseSalary());
            row.put("performanceSalary", r.getPerformanceSalary());
            row.put("grossAmount", r.getGrossAmount());
            row.put("social", r.getDeductionTotal() != null
                    ? r.getDeductionTotal().subtract(r.getTax() == null ? BigDecimal.ZERO : r.getTax()) : BigDecimal.ZERO);
            row.put("tax", r.getTax());
            row.put("netAmount", r.getNetAmount());
            row.put("status", r.getStatus());
            rows.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", month);
        result.put("rows", rows);
        result.put("count", rows.size());
        return result;
    }

    /** 当月汇总 + 成本构成 */
    @Transactional(readOnly = true)
    public Map<String, Object> summary(String month) {
        List<PayrollRecord> records = payrollRepository.findByPayMonth(month);
        BigDecimal gross = sum(records, PayrollRecord::getGrossAmount);
        BigDecimal net = sum(records, PayrollRecord::getNetAmount);
        BigDecimal social = sum(records, PayrollRecord::getDeductionTotal).subtract(sum(records, PayrollRecord::getTax));
        BigDecimal tax = sum(records, PayrollRecord::getTax);
        BigDecimal base = sum(records, PayrollRecord::getBaseSalary);
        BigDecimal perf = sum(records, PayrollRecord::getPerformanceSalary);
        BigDecimal position = sum(records, PayrollRecord::getPositionSalary);
        BigDecimal allowance = sum(records, PayrollRecord::getAllowance);
        BigDecimal overtime = sum(records, PayrollRecord::getOvertimePay);
        BigDecimal bonus = sum(records, PayrollRecord::getBonus);

        long calculated = records.stream().filter(r -> "CALCULATED".equals(r.getStatus())).count();
        long draft = records.stream().filter(r -> "DRAFT".equals(r.getStatus())).count();
        long paid = records.stream().filter(r -> "PAID".equals(r.getStatus())).count();
        long confirmed = records.stream().filter(r -> "CONFIRMED".equals(r.getStatus())).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", month);
        result.put("employeeCount", records.size());
        result.put("totalGross", gross);
        result.put("totalNet", net);
        result.put("totalSocial", social);
        result.put("totalTax", tax);
        result.put("avgNet", records.isEmpty() ? BigDecimal.ZERO : net.divide(BigDecimal.valueOf(records.size()), 0, RoundingMode.HALF_UP));
        result.put("statusCount", Map.of("DRAFT", draft, "CALCULATED", calculated, "CONFIRMED", confirmed, "PAID", paid));
        result.put("costBreakdown", Map.of(
                "base", base, "position", position, "performance", perf,
                "allowance", allowance, "overtime", overtime, "bonus", bonus));
        return result;
    }

    /** 部门人力成本 */
    @Transactional(readOnly = true)
    public Map<String, Object> deptCost(String month) {
        List<PayrollRecord> records = payrollRepository.findByPayMonth(month);
        Map<Long, Employee> empMap = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));
        Map<String, BigDecimal> costByDept = new LinkedHashMap<>();
        for (PayrollRecord r : records) {
            Employee e = empMap.get(r.getEmployeeId());
            String dept = e != null && e.getDepartment() != null ? e.getDepartment().getName() : "未分配";
            costByDept.merge(dept, r.getGrossAmount() == null ? BigDecimal.ZERO : r.getGrossAmount(), BigDecimal::add);
        }
        List<Map<String, Object>> list = costByDept.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(en -> Map.<String, Object>of("name", en.getKey(), "value", en.getValue()))
                .collect(Collectors.toList());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", month);
        result.put("items", list);
        return result;
    }

    /** 工资条 */
    @Transactional(readOnly = true)
    public Map<String, Object> payslip(Long employeeId, String month) {
        PayrollRecord r = payrollRepository.findByEmployeeIdAndPayMonth(employeeId, month)
                .orElseThrow(() -> new BizException("该月工资条不存在"));
        Employee e = employeeRepository.findById(employeeId).orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("record", r);
        result.put("employee", e != null ? Map.of(
                "name", e.getName(), "employeeNo", e.getEmployeeNo(),
                "department", e.getDepartment() != null ? e.getDepartment().getName() : "-",
                "position", e.getPosition() != null ? e.getPosition().getName() : "-",
                "level", e.getLevel(), "hireDate", e.getHireDate()) : Map.of());
        return result;
    }

    /** 模拟核算：为指定月份生成/重算薪酬记录 */
    @Transactional
    public int calculate(String month) {
        List<PayrollRecord> existing = payrollRepository.findByPayMonth(month);
        // 已确认/已发放月份禁止重算，避免静默撤销发放状态
        boolean paid = existing.stream().anyMatch(r -> "PAID".equals(r.getStatus()));
        boolean confirmed = existing.stream().anyMatch(r -> "CONFIRMED".equals(r.getStatus()));
        if (paid) throw new BizException("该月薪酬已发放，不能重新核算");
        if (confirmed) throw new BizException("该月薪酬已确认，如需调整请先撤销确认");
        // 删除该月已有记录后重建
        payrollRepository.deleteAll(existing);
        payrollRepository.flush();

        List<Employee> active = employeeRepository.findByStatusNotOrderByHireDateAsc("LEFT");
        List<PayrollRecord> batch = new ArrayList<>();
        for (Employee e : active) {
            PayrollRecord p = new PayrollRecord();
            p.setEmployeeId(e.getId());
            p.setPayMonth(month);
            BigDecimal base = e.getBaseSalary() != null ? e.getBaseSalary() : BigDecimal.valueOf(12000);
            p.setBaseSalary(base);
            p.setPositionSalary(base.multiply(BigDecimal.valueOf(0.15)).setScale(0, RoundingMode.HALF_UP));
            p.setPerformanceSalary(base.multiply(BigDecimal.valueOf(0.3)).setScale(0, RoundingMode.HALF_UP));
            p.setAllowance(BigDecimal.ZERO);
            p.setOvertimePay(BigDecimal.ZERO);
            p.setBonus(BigDecimal.ZERO);
            BigDecimal gross = p.getBaseSalary().add(p.getPositionSalary()).add(p.getPerformanceSalary());
            p.setGrossAmount(gross);
            BigDecimal social = gross.multiply(BigDecimal.valueOf(0.105)).setScale(0, RoundingMode.HALF_UP);
            BigDecimal fund = gross.multiply(BigDecimal.valueOf(0.12)).setScale(0, RoundingMode.HALF_UP);
            BigDecimal taxable = gross.subtract(social).subtract(fund).subtract(BigDecimal.valueOf(5000));
            BigDecimal tax = taxable.compareTo(BigDecimal.ZERO) > 0
                    ? taxable.multiply(BigDecimal.valueOf(0.1)).setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            p.setSocialPension(social.multiply(BigDecimal.valueOf(0.52)).setScale(0, RoundingMode.HALF_UP));
            p.setSocialMedical(social.multiply(BigDecimal.valueOf(0.33)).setScale(0, RoundingMode.HALF_UP));
            p.setSocialUnemployment(social.multiply(BigDecimal.valueOf(0.15)).setScale(0, RoundingMode.HALF_UP));
            p.setHousingFund(fund);
            p.setTax(tax);
            p.setDeduction(BigDecimal.ZERO);
            p.setDeductionTotal(social.add(fund).add(tax));
            p.setNetAmount(gross.subtract(social).subtract(fund).subtract(tax));
            p.setStatus("CALCULATED");
            p.setOperatorId(1L);
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            batch.add(p);
        }
        payrollRepository.saveAll(batch);
        return batch.size();
    }

    /** 更新单条薪酬记录状态（确认 / 发放），供前端“确认/发放”操作持久化 */
    @Transactional
    public void updateStatus(Long id, String status) {
        PayrollRecord r = payrollRepository.findById(id)
                .orElseThrow(() -> new BizException("薪酬记录不存在"));
        if (!List.of("CALCULATED", "CONFIRMED", "PAID", "DRAFT").contains(status)) {
            throw new BizException("非法的薪酬状态：" + status);
        }
        if ("PAID".equals(status) && !"CONFIRMED".equals(r.getStatus())) {
            throw new BizException("请先确认再发放");
        }
        r.setStatus(status);
        r.setUpdatedAt(LocalDateTime.now());
        payrollRepository.save(r);
    }

    private BigDecimal sum(List<PayrollRecord> records, java.util.function.Function<PayrollRecord, BigDecimal> getter) {
        return records.stream().map(getter).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
