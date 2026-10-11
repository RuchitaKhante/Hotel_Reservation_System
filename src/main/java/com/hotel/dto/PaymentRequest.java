package com.hotel.dto;

public record PaymentRequest(Long bookingId, String paymentMethod) {}
