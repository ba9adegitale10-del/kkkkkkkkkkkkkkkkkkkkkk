package com.example.megrine.controller;

import com.example.megrine.model.*;
import com.example.megrine.repository.*;
import com.example.megrine.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/member")
public class MemberController {

    @Autowired private UserRepository userRepo;
    @Autowired private VolunteerRepository volunteerRepo;
    @Autowired private EventParticipationRepository participationRepo;
    @Autowired private TrainingResourceRepository trainingRepo;
    @Autowired private ActivityLogService logService;

    // SECURITE: on utilise Authentication pour obtenir l'utilisateur connecte
    // -> impossible d'acceder au profil d'un autre membre
    private User getCurrentUser(Authentication auth) {
        return userRepo.findByUsername(auth.getName()).orElseThrow();
    }

    // MON ESPACE - Dashboard personnel
    @GetMapping
    public String monEspace(Authentication auth, Model model) {
        User user = getCurrentUser(auth);
        model.addAttribute("user", user);

        // Si le compte a un profil benevole lie
        if (user.getVolunteer() != null) {
            Volunteer vol = user.getVolunteer();
            model.addAttribute("volunteer", vol);

            // Heures calculees depuis les participations completees
            Integer hours = participationRepo.sumHoursByVolunteerId(vol.getId());
            model.addAttribute("totalHours", hours != null ? hours : 0);
            model.addAttribute("badge", vol.getComputedBadge());

            // Mes participations
            List<EventParticipation> participations =
                participationRepo.findByVolunteerIdOrderByEnrolledAtDesc(vol.getId());
            model.addAttribute("participations", participations);
        }

        // Ressources de formation
        model.addAttribute("resources", trainingRepo.findByVisibleTrueOrderByUploadedAtDesc());

        logService.log("Consultation Mon Espace", ActivityLog.ActionType.VIEW,
            "Membre", user.getUsername(), null);
        return "member/dashboard";
    }

    // DISPONIBILITES - Afficher formulaire
    @GetMapping("/availability")
    public String availabilityForm(Authentication auth, Model model) {
        User user = getCurrentUser(auth);
        model.addAttribute("user", user);
        if (user.getVolunteer() != null) {
            String avail = user.getVolunteer().getAvailability();
            List<String> selected = avail != null && !avail.isBlank() ?
                Arrays.asList(avail.split(",")) : List.of();
            model.addAttribute("selectedSlots", selected);
        }
        return "member/availability";
    }

    // DISPONIBILITES - Sauvegarder
    // SECURITE: validation stricte des valeurs acceptees (whitelist)
    @PostMapping("/availability")
    public String saveAvailability(Authentication auth,
            @RequestParam(value="slots", required=false) List<String> slots,
            RedirectAttributes ra) {

        User user = getCurrentUser(auth);
        if (user.getVolunteer() == null) {
            ra.addFlashAttribute("error", "Votre compte n'est pas lie a un profil benevole.");
            return "redirect:/member";
        }

        // WHITELIST des valeurs autorisees - protection contre injection
        List<String> ALLOWED = List.of(
            "LUNDI_MATIN","LUNDI_SOIREE",
            "MARDI_MATIN","MARDI_SOIREE",
            "MERCREDI_MATIN","MERCREDI_SOIREE",
            "JEUDI_MATIN","JEUDI_SOIREE",
            "VENDREDI_MATIN","VENDREDI_SOIREE",
            "SAMEDI","DIMANCHE","WEEKEND","SOIREE","MATIN"
        );

        String sanitized = "";
        if (slots != null && !slots.isEmpty()) {
            sanitized = slots.stream()
                .filter(ALLOWED::contains) // Seules les valeurs whitelist passent
                .distinct()
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        }

        Volunteer vol = user.getVolunteer();
        vol.setAvailability(sanitized);
        volunteerRepo.save(vol);

        logService.log("Mise a jour disponibilites: " + user.getUsername(),
            ActivityLog.ActionType.UPDATE, "Disponibilite", user.getUsername(),
            "Creneaux: " + sanitized);

        ra.addFlashAttribute("success", "Disponibilites mises a jour !");
        return "redirect:/member";
    }
}
