package com.example.megrine.controller;

import com.example.megrine.model.*;
import com.example.megrine.repository.*;
import com.example.megrine.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;

@Controller
public class ParticipationController {

    @Autowired private EventRepository eventRepo;
    @Autowired private VolunteerRepository volunteerRepo;
    @Autowired private EventParticipationRepository participationRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private ActivityLogService logService;

    // INSCRIPTION a un evenement
    // SECURITE: verifie double inscription + capacite max
    @PostMapping("/events/participate/{eventId}")
    public String participate(@PathVariable Long eventId,
            Authentication auth, RedirectAttributes ra) {
        try {
            User user = userRepo.findByUsername(auth.getName()).orElseThrow();

            // L'utilisateur doit avoir un profil benevole
            if (user.getVolunteer() == null) {
                ra.addFlashAttribute("error", "Votre compte n'est pas lie a un profil benevole.");
                return "redirect:/events";
            }

            Volunteer vol = user.getVolunteer();
            Event event = eventRepo.findById(eventId).orElseThrow();

            // SECURITE 1: Verifier double inscription (contrainte DB + check applicatif)
            if (participationRepo.existsByEventIdAndVolunteerId(eventId, vol.getId())) {
                ra.addFlashAttribute("error", "Vous etes deja inscrit a cet evenement !");
                return "redirect:/events";
            }

            // SECURITE 2: Verifier capacite max de l'evenement
            if (event.getParticipantsCount() != null) {
                long enrolled = participationRepo.countByEventId(eventId);
                if (enrolled >= event.getParticipantsCount()) {
                    ra.addFlashAttribute("error", "Cet evenement est complet (" + event.getParticipantsCount() + " participants max).");
                    return "redirect:/events";
                }
            }

            // SECURITE 3: Evenement doit etre a venir ou en cours
            if (event.getStatus() == Event.EventStatus.COMPLETED ||
                event.getStatus() == Event.EventStatus.CANCELLED) {
                ra.addFlashAttribute("error", "Cet evenement est termine ou annule.");
                return "redirect:/events";
            }

            // Creer la participation
            EventParticipation p = new EventParticipation();
            p.setEvent(event);
            p.setVolunteer(vol);
            p.setEnrolledAt(LocalDateTime.now());
            p.setStatus(EventParticipation.ParticipationStatus.ENROLLED);
            participationRepo.save(p);

            logService.log("Inscription evenement: " + event.getTitle(),
                ActivityLog.ActionType.CREATE, "Participation", vol.getFullName(),
                "Evenement: " + event.getTitle() + " | Date: " + event.getEventDate());

            ra.addFlashAttribute("success", "Inscription confirmee pour: " + event.getTitle() + " !");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de l'inscription.");
        }
        return "redirect:/events";
    }

    // ANNULER son inscription
    @PostMapping("/events/cancel/{eventId}")
    public String cancel(@PathVariable Long eventId,
            Authentication auth, RedirectAttributes ra) {
        try {
            User user = userRepo.findByUsername(auth.getName()).orElseThrow();
            if (user.getVolunteer() == null) { return "redirect:/events"; }

            Volunteer vol = user.getVolunteer();
            EventParticipation p = participationRepo
                .findByEventIdAndVolunteerId(eventId, vol.getId())
                .orElseThrow();

            // SECURITE: verifier que c'est bien SA participation
            if (!p.getVolunteer().getId().equals(vol.getId())) {
                ra.addFlashAttribute("error", "Action non autorisee.");
                return "redirect:/events";
            }

            p.setStatus(EventParticipation.ParticipationStatus.CANCELLED);
            participationRepo.save(p);

            logService.log("Annulation inscription: " + p.getEvent().getTitle(),
                ActivityLog.ActionType.UPDATE, "Participation", vol.getFullName(),
                "Evenement: " + p.getEvent().getTitle());

            ra.addFlashAttribute("success", "Inscription annulee.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de l'annulation.");
        }
        return "redirect:/events";
    }

    // ADMIN: marquer une participation comme completee + ajouter heures
    @PostMapping("/events/participants/{participationId}/complete")
    public String complete(@PathVariable Long participationId,
            @RequestParam(value="hours", defaultValue="0") Integer hours,
            RedirectAttributes ra) {
        try {
            EventParticipation p = participationRepo.findById(participationId).orElseThrow();
            p.setStatus(EventParticipation.ParticipationStatus.COMPLETED);
            p.setHoursContributed(hours);
            participationRepo.save(p);

            // Mettre a jour les heures totales du benevole
            Volunteer vol = p.getVolunteer();
            Integer totalHours = participationRepo.sumHoursByVolunteerId(vol.getId());
            vol.setTotalHours(totalHours != null ? totalHours : 0);
            volunteerRepo.save(vol);

            logService.log("Participation validee: " + vol.getFullName(),
                ActivityLog.ActionType.UPDATE, "Participation", vol.getFullName(),
                "Evenement: " + p.getEvent().getTitle() + " | " + hours + "h | Total: " + vol.getTotalHours() + "h | Badge: " + vol.getComputedBadge());

            ra.addFlashAttribute("success", hours + "h ajoutees a " + vol.getFullName() + " (Badge: " + vol.getComputedBadge() + ")");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur validation.");
        }
        return "redirect:/admin/dashboard";
    }
}
