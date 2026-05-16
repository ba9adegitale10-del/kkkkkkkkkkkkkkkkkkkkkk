package com.example.megrine.controller;

import com.example.megrine.model.User;
import com.example.megrine.model.Volunteer;
import com.example.megrine.model.ActivityLog;
import com.example.megrine.repository.UserRepository;
import com.example.megrine.repository.VolunteerRepository;
import com.example.megrine.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
public class RegistrationController {

    @Autowired private UserRepository userRepo;
    @Autowired private VolunteerRepository volunteerRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ActivityLogService logService;

    @GetMapping("/register")
    public String registerForm() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("fullName") String fullName,
            @RequestParam(value="email", required=false) String email,
            @RequestParam(value="phone", required=false) String phone,
            @RequestParam(value="cin", required=false) String cin,
            @RequestParam(value="age", required=false) Integer age,
            @RequestParam(value="bloodType", required=false) String bloodType,
            @RequestParam(value="address", required=false) String address,
            @RequestParam(value="registrationNote", required=false) String note,
            RedirectAttributes ra) {

        // Validations
        if (userRepo.existsByUsername(username.trim().toLowerCase())) {
            ra.addFlashAttribute("error", "Ce nom d'utilisateur est deja utilise.");
            return "redirect:/register";
        }
        if (username.length() < 3 || password.length() < 6) {
            ra.addFlashAttribute("error", "Username min 3 caracteres, mot de passe min 6.");
            return "redirect:/register";
        }
        if (!username.matches("[a-zA-Z0-9_]+")) {
            ra.addFlashAttribute("error", "Username invalide (lettres, chiffres et _ uniquement).");
            return "redirect:/register";
        }

        // 1. Creer le profil Benevole automatiquement
        String[] nameParts = fullName.trim().split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        Volunteer volunteer = new Volunteer();
        volunteer.setFirstName(firstName);
        volunteer.setLastName(lastName);
        volunteer.setEmail(email);
        volunteer.setPhone(phone);
        volunteer.setCin(cin); // sera ignore si le champ n'existe pas encore
        volunteer.setAddress(address);
        volunteer.setBloodType((bloodType != null && !bloodType.isBlank()) ? bloodType : null);
        volunteer.setJoinDate(LocalDate.now());
        volunteer.setActive(false); // Inactif jusqu'a approbation
        volunteer.setStatus(Volunteer.VolunteerStatus.PENDING);
        volunteer.setTotalHours(0);
        volunteer.setPoints(0);
        volunteer.setBadges("");
        volunteer.setAvailability("");
        Volunteer savedVolunteer = volunteerRepo.save(volunteer);

        // 2. Creer le compte User lie au benevole
        User user = new User();
        user.setUsername(username.trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName.trim());
        user.setEmail(email);
        user.setPhone(phone);
        user.setCin(cin);
        user.setAge(age);
        user.setBloodType(bloodType);
        user.setAddress(address);
        user.setRole("ROLE_MEMBER");
        user.setEnabled(false); // Desactive jusqu'a approbation
        user.setAccountStatus(User.AccountStatus.PENDING);
        user.setPoints(0);
        user.setRegisteredAt(LocalDateTime.now());
        user.setRegistrationNote(note);
        user.setPermissions("EVENTS,TRAINING,MEMBER");
        user.setVolunteer(savedVolunteer); // Lien automatique
        userRepo.save(user);

        logService.log("Nouvelle demande: " + username,
            ActivityLog.ActionType.CREATE, "Inscription", username,
            "Nom: " + fullName + " | Tel: " + phone + " | Email: " + email +
            " | Groupe: " + bloodType + " | Age: " + age);

        return "redirect:/register?sent=true";
    }

    // Admin: voir les demandes
    @GetMapping("/admin/registrations")
    public String pendingList(Model model) {
        model.addAttribute("pending", userRepo.findByAccountStatus(User.AccountStatus.PENDING));
        model.addAttribute("rejected", userRepo.findByAccountStatus(User.AccountStatus.REJECTED));
        return "admin/registrations";
    }

    // Admin: approuver → active le user + le benevole
    @PostMapping("/admin/registrations/approve/{id}")
    public String approve(@PathVariable Long id,
            @RequestParam(value="permissions", required=false) String permissions,
            org.springframework.security.core.Authentication auth,
            RedirectAttributes ra) {
        try {
            User user = userRepo.findById(id).orElseThrow();

            // Activer le compte user
            user.setEnabled(true);
            user.setAccountStatus(User.AccountStatus.ACTIVE);
            user.setApprovedAt(LocalDateTime.now());
            user.setApprovedBy(auth.getName());
            if (permissions != null && !permissions.isBlank()) {
                user.setPermissions(permissions);
            }
            userRepo.save(user);

            // Activer aussi le profil benevole
            if (user.getVolunteer() != null) {
                Volunteer vol = user.getVolunteer();
                vol.setActive(true);
                vol.setStatus(Volunteer.VolunteerStatus.ACTIVE);
                volunteerRepo.save(vol);
            }

            logService.log("Inscription approuvee: " + user.getUsername(),
                ActivityLog.ActionType.UPDATE, "Inscription", user.getUsername(),
                "Approuve par: " + auth.getName() + " | Benevole cree automatiquement");

            ra.addFlashAttribute("success",
                user.getFullName() + " approuve ! Profil benevole active automatiquement.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/admin/registrations";
    }

    // Admin: refuser
    @PostMapping("/admin/registrations/reject/{id}")
    public String reject(@PathVariable Long id,
            org.springframework.security.core.Authentication auth,
            RedirectAttributes ra) {
        try {
            User user = userRepo.findById(id).orElseThrow();
            user.setAccountStatus(User.AccountStatus.REJECTED);
            user.setApprovedBy(auth.getName());
            userRepo.save(user);

            // Desactiver aussi le benevole
            if (user.getVolunteer() != null) {
                user.getVolunteer().setActive(false);
                volunteerRepo.save(user.getVolunteer());
            }

            logService.log("Inscription refusee: " + user.getUsername(),
                ActivityLog.ActionType.UPDATE, "Inscription", user.getUsername(),
                "Refuse par: " + auth.getName());

            ra.addFlashAttribute("success", "Demande refusee.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur.");
        }
        return "redirect:/admin/registrations";
    }
}
