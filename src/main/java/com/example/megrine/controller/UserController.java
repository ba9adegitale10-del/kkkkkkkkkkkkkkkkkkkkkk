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

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired private UserRepository userRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ActivityLogService logService;

    @GetMapping
    public String list(Model model) { model.addAttribute("users", userRepo.findAll()); return "users/list"; }

    @GetMapping("/new")
    public String newForm(Model model) { model.addAttribute("user", new User()); return "users/form"; }

    @PostMapping("/save")
    public String save(@RequestParam(value="id",required=false) Long id,
            @RequestParam("username") String username,
            @RequestParam(value="password",required=false) String password,
            @RequestParam(value="fullName",required=false) String fullName,
            @RequestParam(value="email",required=false) String email,
            @RequestParam(value="role",defaultValue="ROLE_USER") String role,
            @RequestParam(value="enabled",required=false) String enabled,
            RedirectAttributes ra) {
        boolean isNew = (id == null);
        User user = isNew ? new User() : userRepo.findById(id).orElse(new User());
        user.setUsername(username); user.setFullName(fullName); user.setEmail(email); user.setRole(role);
        user.setEnabled("true".equals(enabled) || "on".equals(enabled));
        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        } else if (isNew) {
            ra.addFlashAttribute("error", "Le mot de passe est obligatoire.");
            return "redirect:/users/new";
        }
        userRepo.save(user);
        String details = "Role: " + role + " | Email: " + email + " | Actif: " + user.isEnabled();
        if (isNew) logService.log("Creation compte: " + username, ActivityLog.ActionType.CREATE, "Utilisateur", username, details);
        else logService.log("Modification compte: " + username, ActivityLog.ActionType.UPDATE, "Utilisateur", username, details);
        ra.addFlashAttribute("success", "Utilisateur enregistre !");
        return "redirect:/users";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) { model.addAttribute("user", userRepo.findById(id).orElseThrow()); return "users/form"; }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            User u = userRepo.findById(id).orElseThrow();
            String uname = u.getUsername();
            userRepo.deleteById(id);
            logService.log("Suppression compte: " + uname, ActivityLog.ActionType.DELETE, "Utilisateur", uname, "Role: " + u.getRole());
            ra.addFlashAttribute("success", "Utilisateur supprime.");
        } catch (Exception e) { ra.addFlashAttribute("error", "Impossible de supprimer."); }
        return "redirect:/users";
    }

    @GetMapping("/toggle/{id}")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        try {
            User user = userRepo.findById(id).orElseThrow();
            user.setEnabled(!user.isEnabled());
            userRepo.save(user);
            logService.log((user.isEnabled()?"Activation":"Desactivation") + " compte: " + user.getUsername(),
                ActivityLog.ActionType.UPDATE, "Utilisateur", user.getUsername(),
                "Compte " + (user.isEnabled()?"active":"desactive"));
            ra.addFlashAttribute("success", user.isEnabled() ? "Compte active." : "Compte desactive.");
        } catch (Exception e) { ra.addFlashAttribute("error", "Erreur."); }
        return "redirect:/users";
    }
}
