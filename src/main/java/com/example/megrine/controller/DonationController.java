package com.example.megrine.controller;

import com.example.megrine.model.Donation;
import com.example.megrine.repository.DonationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/donations")
public class DonationController {

    @Autowired
    private DonationRepository donationRepo;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("donations", donationRepo.findAll());
        model.addAttribute("totalAmount", donationRepo.sumMonetaryDonations());
        return "donations/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        Donation d = new Donation();
        d.setType(Donation.DonationType.MONETARY);
        d.setStatus(Donation.DonationStatus.RECEIVED);
        model.addAttribute("donation", d);
        return "donations/form";
    }

    @PostMapping("/save")
    public String save(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "donorName", required = false) String donorName,
            @RequestParam(value = "donorEmail", required = false) String donorEmail,
            @RequestParam(value = "donorPhone", required = false) String donorPhone,
            @RequestParam(value = "amount", required = false) String amount,
            @RequestParam(value = "type", defaultValue = "MONETARY") String type,
            @RequestParam(value = "status", defaultValue = "RECEIVED") String status,
            @RequestParam(value = "donationDate", required = false) String donationDate,
            @RequestParam(value = "description", required = false) String description,
            RedirectAttributes ra) {

        Donation donation = (id != null) ? donationRepo.findById(id).orElse(new Donation()) : new Donation();
        donation.setDonorName(donorName);
        donation.setDonorEmail(donorEmail);
        donation.setDonorPhone(donorPhone);
        donation.setDescription(description);

        try { donation.setAmount(new BigDecimal(amount)); }
        catch (Exception e) { donation.setAmount(BigDecimal.ZERO); }

        try { donation.setType(Donation.DonationType.valueOf(type)); }
        catch (Exception e) { donation.setType(Donation.DonationType.MONETARY); }

        try { donation.setStatus(Donation.DonationStatus.valueOf(status)); }
        catch (Exception e) { donation.setStatus(Donation.DonationStatus.RECEIVED); }

        if (donationDate != null && !donationDate.isBlank()) {
            try { donation.setDonationDate(LocalDate.parse(donationDate)); }
            catch (Exception ignored) {}
        } else if (donation.getDonationDate() == null) {
            donation.setDonationDate(LocalDate.now());
        }

        donationRepo.save(donation);
        ra.addFlashAttribute("success", "Don enregistré avec succès !");
        return "redirect:/donations";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("donation", donationRepo.findById(id).orElseThrow());
        return "donations/form";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        donationRepo.deleteById(id);
        ra.addFlashAttribute("success", "Don supprimé.");
        return "redirect:/donations";
    }
}
