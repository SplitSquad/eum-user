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
    public ResponseEntity<?> getEvents(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(googleCalendarService.getUpcomingEvents(token));
    }

    @PostMapping
    public ResponseEntity<?> insert(@RequestHeader("Authorization") String token,
                                    @RequestBody GoogleCalendarEventRequestDto eventDto){
        return googleCalendarService.insertEvent(token, eventDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@RequestHeader("Authorization") String token,
                                         @PathVariable String id) {
        return googleCalendarService.deleteEvent(token, id);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateEvent(@RequestHeader("Authorization") String token,
                                         @PathVariable String id,
                                         @RequestBody GoogleCalendarEventRequestDto eventDto) {
        return googleCalendarService.updateEvent(token, id, eventDto);
    }

}
