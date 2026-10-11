package com.hotel.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    private double amount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;  // CREDIT_CARD, DEBIT_CARD, UPI, CASH

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;  // SUCCESS, FAILED, PENDING

    private String transactionId;
    private LocalDateTime paymentTime;

    // Enums (OOP requirement)
    public enum PaymentMethod {
        CREDIT_CARD, DEBIT_CARD, UPI, CASH
    }

    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED
    }

    // ─── Constructors ───────────────────────────────────────────────
    public Payment() {}

    // ─── Getters & Setters ──────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public LocalDateTime getPaymentTime() { return paymentTime; }
    public void setPaymentTime(LocalDateTime paymentTime) { this.paymentTime = paymentTime; }
}