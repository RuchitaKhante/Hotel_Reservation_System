package com.hotel.controller;

import com.hotel.dto.BookingRequest;
import com.hotel.dto.BookingResponse;
import com.hotel.entity.Booking;
import com.hotel.service.BookingService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    static boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // POST /api/bookings   (logged-in users)
    @PostMapping
    public ResponseEntity<BookingResponse> create(@RequestBody BookingRequest req, Authentication auth) {
        Booking saved = bookingService.createBooking(
                auth.getName(),
                req.roomId(),
                req.guestName(),
                req.guestEmail(),
                req.guestPhone(),
                LocalDate.parse(req.checkInDate()),
                LocalDate.parse(req.checkOutDate()),
                req.numGuests());
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.from(saved));
    }

    // GET /api/bookings/my
    @GetMapping("/my")
    public List<BookingResponse> myBookings(Authentication auth) {
        return bookingService.getBookingsForUser(auth.getName())
                .stream().map(BookingResponse::from).toList();
    }

    // GET /api/bookings/{id}
    @GetMapping("/{id}")
    public BookingResponse one(@PathVariable Long id, Authentication auth) {
        return BookingResponse.from(bookingService.getBookingForUser(id, auth.getName(), isAdmin(auth)));
    }

    // POST /api/bookings/{id}/cancel
    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id, Authentication auth) {
        return BookingResponse.from(bookingService.cancelBooking(id, auth.getName(), isAdmin(auth)));
    }
}
