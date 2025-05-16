package com.server1.controller;

import com.server1.dto.ReportSimpleRes;
import com.server1.entity.ReportEntity;
import com.server1.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<List<ReportSimpleRes>> getAllReports() {
        return ResponseEntity.ok(adminService.getAllReports());
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ReportEntity> getReportDetail(@PathVariable Long reportId) {
        return ResponseEntity.ok(adminService.getReportDetail(reportId));
    }

    @PostMapping("/deactivate/{userId}")
    public ResponseEntity<Void> deactivateUserTemporarily(@PathVariable Long userId) {
        adminService.deactivateTemporarily(userId);
        return ResponseEntity.ok().build();
    }
}
