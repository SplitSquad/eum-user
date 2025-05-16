package com.server1.service;

import com.server1.dto.ReportSimpleRes;
import com.server1.entity.ReportEntity;
import com.server1.entity.UserEntity;
import com.server1.repository.ReportRepository;
import com.server1.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import util.RedisUtil;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final RedisUtil redisUtil;

    public List<ReportSimpleRes> getAllReports() {
        return reportRepository.findAll()
                .stream()
                .map(ReportSimpleRes::from)
                .collect(Collectors.toList());
    }

    public ReportEntity getReportDetail(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신고 내역을 찾을 수 없습니다."));
    }

    @Transactional
    public void deactivateTemporarily(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        user.setIsDeactivate(true); // DB에 isDeactivate = true 저장
        userRepository.save(user);

        redisUtil.setTempDeactivate(user.getEmail(), "true", 30); // 30분 동안 임시 비활성화 저장
    }
}
