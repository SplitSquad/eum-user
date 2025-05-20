package com.server1.controller;

import com.server1.dto.*;
import com.server1.entity.ReportEntity;
import com.server1.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/userlist")
    public ResponseEntity<List<UserFullRes>> getAllUsers(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(adminService.getAllUserInfos(token));
    }

    @PostMapping("/reports/search")
    public ResponseEntity<List<ReportSimpleRes>> getReportsByReportedId(
            @RequestBody ReportFilterReq req,
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(adminService.getReportsByReportedId(req.getUserId(), token));
    }

    @GetMapping("/reports/{reportId}")
    public ResponseEntity<ReportEntity> getReportDetail(
            @PathVariable Long reportId,
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(adminService.getReportDetail(reportId, token));
    }

    @PostMapping("/reports/deactivate")
    public ResponseEntity<Void> deactivateUserTemporarily(
            @RequestBody DeactivateReq req,
            @RequestHeader("Authorization") String token) {
        adminService.deactivateTemporarily(req.getUserId(), req.getMinutes(), token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/promote")
    public ResponseEntity<Void> promoteUser(
            @RequestBody PromoteReq req,
            @RequestHeader("Authorization") String token) {
        adminService.promoteUserToAdmin(req.getEmail(), token);
        return ResponseEntity.ok().build();
    }
}
