package com.example.megrine.controller;

import com.example.megrine.model.User;
import com.example.megrine.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired private UserRepository userRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userRepo.findAll());
        return "users/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("user", new User());
        return "users/form";
    }

    @PostMapping("/save")
    public String save(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam("username") String username,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "role", defaultValue = "ROLE_USER") String role,
            @RequestParam(value = "enabled", required = false) String enabled,
            RedirectAttributes ra) {

        User user = (id != null) ? userRepo.findById(id).orElse(new User()) : new User();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(role);
        user.setEnabled("true".equals(enabled) || "on".equals(enabled));

        // Mot de passe : seulement si renseigné
        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        } else if (id == null) {
            ra.addFlashAttribute("error", "Le mot de passe est obligatoire pour un nouvel utilisateur.");
            return "redirect:/users/new";
        }

        userRepo.save(user);
        ra.addFlashAttribute("success", "Utilisateur enregistré avec succès !");
        return "redirect:/users";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("user", userRepo.findById(id).orElseThrow());
        return "users/form";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        userRepo.deleteById(id);
        ra.addFlashAttribute("success", "Utilisateur supprimé.");
        return "redirect:/users";
    }

    @GetMapping("/toggle/{id}")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        User user = userRepo.findById(id).orElseThrow();
        user.setEnabled(!user.isEnabled());
        userRepo.save(user);
        ra.addFlashAttribute("success", user.isEnabled() ? "Compte activé." : "Compte désactivé.");
        return "redirect:/users";
    }
}
