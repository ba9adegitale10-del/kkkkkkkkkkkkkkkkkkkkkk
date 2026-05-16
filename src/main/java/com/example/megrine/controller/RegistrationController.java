package com.example.megrine.controller;

import com.example.megrine.model.User;
import com.example.megrine.model.ActivityLog;
import com.example.megrine.repository.UserRepository;
import com.example.megrine.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;

@Controller
public class RegistrationController {

    @Autowired private UserRepository userRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ActivityLogService logService;

    // Page inscription publique
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    // Soumettre une demande d'inscription
    @PostMapping("/register")
    public String register(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("fullName") String fullName,
            @RequestParam(value="email", required=false) String email,
            @RequestParam(value="phone", required=false) String phone,
            @RequestParam(value="cin", required=false) String cin,
            @RequestParam(value="address", required=false) String address,
            @RequestParam(value="registrationNote", required=false) String note,
            RedirectAttributes ra) {

        // Verifier si username deja pris
        if (userRepo.existsByUsername(username)) {
            ra.addFlashAttribute("error", "Ce nom d'utilisateur est deja utilise.");
            return "redirect:/register";
        }

        // Validation basique
        if (username.length() < 3 || password.length() < 6) {
            ra.addFlashAttribute("error", "Nom d'utilisateur min 3 caracteres, mot de passe min 6.");
            return "redirect:/register";
        }

        // Securite: sanitize username (lettres, chiffres, underscore seulement)
        if (!username.matches("[a-zA-Z0-9_]+")) {
            ra.addFlashAttribute("error", "Nom d'utilisateur invalide (lettres, chiffres et _ uniquement).");
            return "redirect:/register";
        }

        User user = new User();
        user.setUsername(username.trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName.trim());
        user.setEmail(email);
        user.setPhone(phone);
        user.setCin(cin);
        user.setAddress(address);
        user.setRole("ROLE_MEMBER");
        user.setEnabled(false); // Desactive jusqu'a approbation admin
        user.setAccountStatus(User.AccountStatus.PENDING);
        user.setPoints(0);
        user.setRegisteredAt(LocalDateTime.now());
        user.setRegistrationNote(note);
        user.setPermissions("EVENTS,TRAINING,MEMBER"); // Acces limite par defaut
        userRepo.save(user);

        logService.log("Nouvelle demande inscription: " + username,
            ActivityLog.ActionType.CREATE, "Inscription", username,
            "Nom: " + fullName + " | Email: " + email + " | Tel: " + phone);

        ra.addFlashAttribute("success", true);
        return "redirect:/register?sent=true";
    }

    // Admin: voir les demandes en attente
    @GetMapping("/admin/registrations")
    public String pendingList(Model model) {
        model.addAttribute("pending", userRepo.findByAccountStatus(User.AccountStatus.PENDING));
        model.addAttribute("rejected", userRepo.findByAccountStatus(User.AccountStatus.REJECTED));
        return "admin/registrations";
    }

    // Admin: approuver
    @PostMapping("/admin/registrations/approve/{id}")
    public String approve(@PathVariable Long id,
            @RequestParam(value="permissions", required=false) String permissions,
            org.springframework.security.core.Authentication auth,
            RedirectAttributes ra) {
        try {
            User user = userRepo.findById(id).orElseThrow();
            user.setEnabled(true);
            user.setAccountStatus(User.AccountStatus.ACTIVE);
            user.setApprovedAt(LocalDateTime.now());
            user.setApprovedBy(auth.getName());
            if (permissions != null && !permissions.isBlank()) {
                user.setPermissions(permissions);
            }
            userRepo.save(user);
            logService.log("Inscription approuvee: " + user.getUsername(),
                ActivityLog.ActionType.UPDATE, "Inscription", user.getUsername(),
                "Approuve par: " + auth.getName());
            ra.addFlashAttribute("success", user.getFullName() + " a ete approuve !");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de l'approbation.");
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
