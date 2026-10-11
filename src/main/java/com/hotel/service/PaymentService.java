package com.hotel.service;

import com.hotel.entity.Booking;
import com.hotel.entity.Payment;
import com.hotel.repository.PaymentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;

    public PaymentService(PaymentRepository paymentRepository, BookingService bookingService) {
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
    }

    // Simulated payment (no real gateway)
    @Transactional
    public Payment processPayment(Long bookingId, String methodStr, String email, boolean isAdmin) {
        if (bookingId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking id is required.");

        Booking booking = bookingService.getBookingForUser(bookingId, email, isAdmin);

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This booking was cancelled.");
        if (booking.getStatus() == Booking.BookingStatus.CONFIRMED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This booking is already paid.");

        Payment.PaymentMethod method;
        try {
            method = Payment.PaymentMethod.valueOf(methodStr == null ? "" : methodStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment method.");
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalAmount());
        payment.setPaymentMethod(method);
        payment.setPaymentTime(LocalDateTime.now());
        payment.setPaymentStatus(Payment.PaymentStatus.SUCCESS);   // demo: always succeeds
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        Payment saved = paymentRepository.save(payment);
        bookingService.confirmBooking(booking);
        return saved;
    }

    public Payment getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId).orElse(null);
    }
}
