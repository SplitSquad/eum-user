package com.server1.controller;

import com.server1.dto.GoogleCalendarEventRequestDto;
import com.server1.service.GoogleCalendarService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final GoogleCalendarService googleCalendarService;

    @GetMapping
    public ResponseEntity<?> getEvents(HttpServletRequest request) {
        return ResponseEntity.ok(googleCalendarService.getUpcomingEvents(request));
    }

    @PostMapping
    public ResponseEntity<?> insert(HttpServletRequest request,
                                    @RequestBody GoogleCalendarEventRequestDto eventDto){
        return googleCalendarService.insertEvent(request, eventDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(HttpServletRequest request, @PathVariable String id) {
        return googleCalendarService.deleteEvent(request, id);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateEvent(HttpServletRequest request,
                                         @PathVariable String id,
                                         @RequestBody GoogleCalendarEventRequestDto eventDto) {
        return googleCalendarService.updateEvent(request, id, eventDto);
    }

}
