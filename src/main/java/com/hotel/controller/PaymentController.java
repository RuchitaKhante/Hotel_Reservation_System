package com.hotel.controller;

import com.hotel.dto.BookingResponse;
import com.hotel.dto.PaymentRequest;
import com.hotel.entity.Payment;
import com.hotel.service.PaymentService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // POST /api/payments   { "bookingId": 5, "paymentMethod": "UPI" }
    @PostMapping
    public Map<String, Object> pay(@RequestBody PaymentRequest req, Authentication auth) {
        Payment payment = paymentService.processPayment(
                req.bookingId(), req.paymentMethod(), auth.getName(), BookingController.isAdmin(auth));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("transactionId", payment.getTransactionId());
        out.put("paymentStatus", payment.getPaymentStatus().name());
        out.put("amount", payment.getAmount());
        out.put("booking", BookingResponse.from(payment.getBooking()));
        return out;
    }
}
