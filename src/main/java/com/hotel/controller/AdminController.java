package com.hotel.controller;

import com.hotel.dto.BookingResponse;
import com.hotel.service.BookingService;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Everything under /api/admin/** needs ROLE_ADMIN (enforced in SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final BookingService bookingService;

    public AdminController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/bookings")
    public List<BookingResponse> allBookings() {
        return bookingService.getAllBookings().stream().map(BookingResponse::from).toList();
    }

    @DeleteMapping("/bookings/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return Map.of("message", "Booking deleted");
    }
}
