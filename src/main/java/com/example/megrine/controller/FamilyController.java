package com.example.megrine.controller;

import com.example.megrine.model.Family;
import com.example.megrine.model.FamilyAid;
import com.example.megrine.repository.FamilyRepository;
import com.example.megrine.repository.FamilyAidRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/families")
public class FamilyController {

    @Autowired private FamilyRepository familyRepo;
    @Autowired private FamilyAidRepository aidRepo;

    @GetMapping
    public String list(Model model,
                       @RequestParam(required = false) String search,
                       @RequestParam(required = false) String category) {
        java.util.List<Family> families;
        if (search != null && !search.isBlank()) {
            families = familyRepo.findByHeadNameContainingIgnoreCase(search);
            model.addAttribute("search", search);
        } else if (category != null && !category.isBlank()) {
            try { families = familyRepo.findByCategory(Family.FamilyCategory.valueOf(category)); }
            catch (Exception e) { families = familyRepo.findAll(); }
            model.addAttribute("selectedCategory", category);
        } else {
            families = familyRepo.findAll();
        }
        model.addAttribute("families", families);
        model.addAttribute("categories", Family.FamilyCategory.values());
        model.addAttribute("totalFamilies", familyRepo.count());
        model.addAttribute("activeFamilies", familyRepo.countByStatus(Family.FamilyStatus.ACTIVE));
        return "families/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("family", new Family());
        return "families/form";
    }

    @PostMapping("/save")
    public String save(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam("headName") String headName,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "cin", required = false) String cin,
            @RequestParam(value = "membersCount", required = false) Integer membersCount,
            @RequestParam(value = "situation", required = false) String situation,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "category", defaultValue = "NEEDY") String category,
            @RequestParam(value = "status", defaultValue = "ACTIVE") String status,
            @RequestParam(value = "registeredDate", required = false) String registeredDate,
            RedirectAttributes ra) {

        Family family = (id != null) ? familyRepo.findById(id).orElse(new Family()) : new Family();
        family.setHeadName(headName);
        family.setPhone(phone);
        family.setAddress(address);
        family.setCin(cin);
        family.setMembersCount(membersCount);
        family.setSituation(situation);
        family.setNotes(notes);

        try { family.setCategory(Family.FamilyCategory.valueOf(category)); }
        catch (Exception e) { family.setCategory(Family.FamilyCategory.NEEDY); }
        try { family.setStatus(Family.FamilyStatus.valueOf(status)); }
        catch (Exception e) { family.setStatus(Family.FamilyStatus.ACTIVE); }

        if (registeredDate != null && !registeredDate.isBlank()) {
            try { family.setRegisteredDate(LocalDate.parse(registeredDate)); }
            catch (Exception ignored) {}
        } else if (family.getRegisteredDate() == null) {
            family.setRegisteredDate(LocalDate.now());
        }

        familyRepo.save(family);
        ra.addFlashAttribute("success", "Famille enregistree avec succes !");
        return "redirect:/families";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, Model model) {
        Family family = familyRepo.findById(id).orElseThrow();
        model.addAttribute("family", family);
        model.addAttribute("aids", aidRepo.findByFamilyIdOrderByAidDateDesc(id));
        model.addAttribute("newAid", new FamilyAid());
        return "families/view";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("family", familyRepo.findById(id).orElseThrow());
        return "families/form";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            // Supprimer d'abord les aides liées
            java.util.List<com.example.megrine.model.FamilyAid> aids = aidRepo.findByFamilyIdOrderByAidDateDesc(id);
            aidRepo.deleteAll(aids);
            // Puis supprimer la famille
            familyRepo.deleteById(id);
            ra.addFlashAttribute("success", "Famille supprimee avec succes.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur lors de la suppression.");
        }
        return "redirect:/families";
    }

    // Ajouter une aide à une famille
    @PostMapping("/aid/save")
    public String saveAid(
            @RequestParam("familyId") Long familyId,
            @RequestParam(value = "type", defaultValue = "FOOD") String type,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "amount", required = false) String amount,
            @RequestParam(value = "quantity", required = false) Integer quantity,
            @RequestParam(value = "unit", required = false) String unit,
            @RequestParam(value = "givenBy", required = false) String givenBy,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "aidDate", required = false) String aidDate,
            RedirectAttributes ra) {

        FamilyAid aid = new FamilyAid();
        aid.setFamily(familyRepo.findById(familyId).orElseThrow());
        aid.setDescription(description);
        aid.setQuantity(quantity);
        aid.setUnit(unit);
        aid.setGivenBy(givenBy);
        aid.setNotes(notes);

        try { aid.setType(FamilyAid.AidType.valueOf(type)); }
        catch (Exception e) { aid.setType(FamilyAid.AidType.OTHER); }

        if (amount != null && !amount.isBlank()) {
            try { aid.setAmount(new BigDecimal(amount)); } catch (Exception ignored) {}
        }
        if (aidDate != null && !aidDate.isBlank()) {
            try { aid.setAidDate(LocalDate.parse(aidDate)); }
            catch (Exception ignored) {}
        } else {
            aid.setAidDate(LocalDate.now());
        }

        aidRepo.save(aid);
        ra.addFlashAttribute("success", "Aide enregistree avec succes !");
        return "redirect:/families/view/" + familyId;
    }
}
