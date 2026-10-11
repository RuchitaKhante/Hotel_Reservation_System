package com.hotel.service;

import com.hotel.entity.Booking;
import com.hotel.entity.Room;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.PaymentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RoomService roomService;

    public BookingService(BookingRepository bookingRepository,
                          PaymentRepository paymentRepository,
                          RoomService roomService) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.roomService = roomService;
    }

    // ───────── create ─────────
    @Transactional
    public Booking createBooking(String userEmail, Long roomId, String guestName, String guestEmail,
                                 String guestPhone, LocalDate checkIn, LocalDate checkOut, int numGuests) {

        if (guestName == null || guestName.isBlank())
            throw bad("Guest name is required.");
        if (guestEmail == null || guestEmail.isBlank())
            throw bad("Guest email is required.");
        if (roomId == null)
            throw bad("Please choose a room.");

        Room room = roomService.getRoomById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found."));

        if (!room.isAvailable())
            throw bad("This room is currently not available.");
        if (checkIn.isBefore(LocalDate.now()))
            throw bad("Check-in date cannot be in the past.");
        if (!checkOut.isAfter(checkIn))
            throw bad("Check-out must be at least one day after check-in.");
        if (numGuests < 1 || numGuests > room.getMaxGuests())
            throw bad("This room allows 1 to " + room.getMaxGuests() + " guests.");

        if (bookingRepository.countOverlapping(roomId, checkIn, checkOut, Booking.BookingStatus.CANCELLED) > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This room is already booked for those dates. Try other dates or another room.");

        Booking booking = new Booking();
        booking.setUserEmail(userEmail);
        booking.setRoom(room);
        booking.setGuestName(guestName.trim());
        booking.setGuestEmail(guestEmail.trim());
        booking.setGuestPhone(guestPhone == null ? null : guestPhone.trim());
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setNumGuests(numGuests);
        booking.setTotalAmount(room.getPricePerNight() * booking.getNumNights());
        booking.setStatus(Booking.BookingStatus.PENDING);   // becomes CONFIRMED after payment

        return bookingRepository.save(booking);
    }

    // ───────── read ─────────
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByIdDesc();
    }

    public List<Booking> getBookingsForUser(String email) {
        return bookingRepository.findByUserEmailOrderByIdDesc(email);
    }

    /** Returns the booking only if it belongs to this user (or the user is an admin). */
    public Booking getBookingForUser(Long id, String email, boolean isAdmin) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found."));
        if (!isAdmin && !email.equalsIgnoreCase(booking.getUserEmail()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This booking belongs to another account.");
        return booking;
    }

    // ───────── update ─────────
    @Transactional
    public Booking confirmBooking(Booking booking) {
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking cancelBooking(Long id, String email, boolean isAdmin) {
        Booking booking = getBookingForUser(id, email, isAdmin);
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED)
            throw bad("This booking is already cancelled.");
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    // ───────── delete (admin) ─────────
    @Transactional
    public void deleteBooking(Long id) {
        if (!bookingRepository.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found.");
        paymentRepository.deleteByBookingId(id); // child first
        bookingRepository.deleteById(id);        // then parent
    }

    private ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}
