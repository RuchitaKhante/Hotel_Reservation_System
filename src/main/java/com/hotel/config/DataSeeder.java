package com.hotel.config;

import com.hotel.entity.Room;
import com.hotel.model.User;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Runs on every startup; only inserts data that is missing. Replaces data.sql. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, RoomRepository roomRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@luxestay.com").isEmpty()) {
            User admin = new User();
            admin.setName("Admin");
            admin.setEmail("admin@luxestay.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ROLE_ADMIN");
            userRepository.save(admin);
        }

        if (roomRepository.count() == 0) {
            String std = "A calm, comfortable retreat with thoughtful essentials and soft natural textures.";
            String dlx = "Extra space, elevated details, and a relaxing atmosphere for a refreshing stay.";
            String ste = "A spacious suite with a separate lounge area and a little extra indulgence.";

            roomRepository.save(new Room("101", Room.RoomType.STANDARD, 2499, true, std, 2));
            roomRepository.save(new Room("102", Room.RoomType.STANDARD, 2499, true, std, 2));
            roomRepository.save(new Room("103", Room.RoomType.STANDARD, 2699, true, std, 2));
            roomRepository.save(new Room("201", Room.RoomType.DELUXE, 4299, true, dlx, 3));
            roomRepository.save(new Room("202", Room.RoomType.DELUXE, 4499, true, dlx, 3));
            roomRepository.save(new Room("301", Room.RoomType.SUITE, 6999, true, ste, 4));
        }

        // Give every room its own image path (also fills rooms created before this field existed)
        for (Room room : roomRepository.findAll()) {
            if (room.getImageUrl() == null || room.getImageUrl().isBlank()) {
                room.setImageUrl("/images/rooms/" + room.getRoomNumber() + ".jpg");
                roomRepository.save(room);
            }
        }
    }
}
