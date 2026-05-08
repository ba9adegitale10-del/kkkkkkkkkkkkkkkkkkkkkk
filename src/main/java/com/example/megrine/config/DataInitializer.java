package com.example.megrine.config;

import com.example.megrine.model.*;
import com.example.megrine.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserRepository userRepo;
    @Autowired private VolunteerRepository volunteerRepo;
    @Autowired private DonationRepository donationRepo;
    @Autowired private StockItemRepository stockRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepo.count() == 0) {
            userRepo.save(new User(null, "admin", passwordEncoder.encode("admin123"), "ROLE_ADMIN", "Administrateur CRT", "admin@crt-megrine.tn", true));
            userRepo.save(new User(null, "user", passwordEncoder.encode("user123"), "ROLE_USER", "Utilisateur CRT", "user@crt-megrine.tn", true));
        }
        if (volunteerRepo.count() == 0) {
            volunteerRepo.save(new Volunteer(null, "Ahmed", "Ben Ali", "ahmed@email.com", "+216 20 123 456", "A+", "Megrine, Ben Arous", LocalDate.of(2022, 3, 15), true, Volunteer.VolunteerStatus.ACTIVE));
            volunteerRepo.save(new Volunteer(null, "Fatima", "Trabelsi", "fatima@email.com", "+216 25 234 567", "B+", "Rades, Ben Arous", LocalDate.of(2021, 6, 10), true, Volunteer.VolunteerStatus.ACTIVE));
            volunteerRepo.save(new Volunteer(null, "Mohamed", "Gharbi", "mohamed@email.com", "+216 52 345 678", "O-", "Hammam Lif", LocalDate.of(2023, 1, 20), true, Volunteer.VolunteerStatus.ACTIVE));
            volunteerRepo.save(new Volunteer(null, "Sarra", "Mansour", "sarra@email.com", "+216 58 456 789", "AB+", "Megrine", LocalDate.of(2020, 9, 5), false, Volunteer.VolunteerStatus.INACTIVE));
            volunteerRepo.save(new Volunteer(null, "Karim", "Bouazizi", "karim@email.com", "+216 23 567 890", "A-", "Ben Arous", LocalDate.of(2023, 11, 1), true, Volunteer.VolunteerStatus.PENDING));
        }
        if (donationRepo.count() == 0) {
            donationRepo.save(new Donation(null, "Société Alpha", "alpha@email.com", "+216 71 111 222", new BigDecimal("5000"), Donation.DonationType.MONETARY, "Don annuel", LocalDate.now().minusDays(5), Donation.DonationStatus.RECEIVED));
            donationRepo.save(new Donation(null, "Famille Ben Salah", null, null, new BigDecimal("500"), Donation.DonationType.MONETARY, "Don Ramadan", LocalDate.now().minusDays(10), Donation.DonationStatus.RECEIVED));
            donationRepo.save(new Donation(null, "Anonyme", null, null, new BigDecimal("200"), Donation.DonationType.FOOD, "Boîtes alimentaires", LocalDate.now().minusDays(2), Donation.DonationStatus.DISTRIBUTED));
            donationRepo.save(new Donation(null, "Association Nour", "nour@email.com", null, new BigDecimal("1500"), Donation.DonationType.CLOTHES, "Vêtements d'hiver", LocalDate.now().minusDays(15), Donation.DonationStatus.RECEIVED));
        }
        if (stockRepo.count() == 0) {
            stockRepo.save(new StockItem(null, "Boîtes alimentaires", "FOOD", "boîte", 145, 20, "Paniers alimentaires Ramadan", "Entrepôt A - Rayon 1", LocalDate.now(), StockItem.StockCategory.FOOD));
            stockRepo.save(new StockItem(null, "Couvertures", "CLOTHES", "pièce", 80, 10, "Couvertures laine hiver", "Entrepôt A - Rayon 2", LocalDate.now(), StockItem.StockCategory.CLOTHES));
            stockRepo.save(new StockItem(null, "Vêtements enfants", "CLOTHES", "kg", 35, 5, "Vêtements 2e main triés", "Entrepôt B", LocalDate.now(), StockItem.StockCategory.CLOTHES));
            stockRepo.save(new StockItem(null, "Médicaments de base", "MEDICAL", "boîte", 8, 15, "Paracétamol, pansements, antiseptique", "Armoire médicale", LocalDate.now(), StockItem.StockCategory.MEDICAL));
            stockRepo.save(new StockItem(null, "Savon / Hygiène", "HYGIENE", "unité", 200, 30, "Savons, shampoings, dentifrices", "Entrepôt A - Rayon 3", LocalDate.now(), StockItem.StockCategory.HYGIENE));
            stockRepo.save(new StockItem(null, "Matelas de camp", "EQUIPMENT", "pièce", 25, 5, "Pour hébergement d'urgence", "Entrepôt B", LocalDate.now(), StockItem.StockCategory.EQUIPMENT));
        }
    }
}
