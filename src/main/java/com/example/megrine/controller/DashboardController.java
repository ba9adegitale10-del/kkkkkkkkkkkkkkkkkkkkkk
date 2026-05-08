package com.example.megrine.controller;

import com.example.megrine.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @Autowired private VolunteerRepository volunteerRepo;
    @Autowired private DonationRepository donationRepo;
    @Autowired private StockItemRepository stockRepo;
    @Autowired private StockMovementRepository movementRepo;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalVolunteers", volunteerRepo.count());
        model.addAttribute("activeVolunteers", volunteerRepo.countByActiveTrue());
        model.addAttribute("totalDonations", donationRepo.count());
        model.addAttribute("totalAmount", donationRepo.sumMonetaryDonations());
        model.addAttribute("totalStockItems", stockRepo.count());
        model.addAttribute("lowStockCount", stockRepo.findLowStock().size());
        model.addAttribute("recentMovements", movementRepo.findTop20ByOrderByMovedAtDesc());
        model.addAttribute("lowStockItems", stockRepo.findLowStock());
        model.addAttribute("volunteers", volunteerRepo.findAll().stream().limit(5).toList());
        return "dashboard/index";
    }
}
