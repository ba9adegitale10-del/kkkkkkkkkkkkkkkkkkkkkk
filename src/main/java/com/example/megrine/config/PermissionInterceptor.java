package com.example.megrine.config;

import com.example.megrine.model.User;
import com.example.megrine.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.Map;

@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Autowired
    private UserRepository userRepo;

    // Mapping URL -> section requise
    private static final Map<String, String> URL_PERMISSIONS = Map.of(
        "/volunteers", "VOLUNTEERS",
        "/families",   "FAMILIES",
        "/donations",  "DONATIONS",
        "/stock",      "STOCK",
        "/events",     "EVENTS",
        "/training",   "TRAINING",
        "/member",     "MEMBER"
    );

    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response, Object handler) throws Exception {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return true;
        if (auth.getName().equals("anonymousUser")) return true;

        // Admin = acces total
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return true;

        String path = request.getRequestURI();

        // Trouver la section correspondante
        String requiredPerm = null;
        for (Map.Entry<String, String> entry : URL_PERMISSIONS.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                requiredPerm = entry.getValue();
                break;
            }
        }

        if (requiredPerm == null) return true; // Pas de restriction

        // Verifier la permission de l'utilisateur
        User user = userRepo.findByUsername(auth.getName()).orElse(null);
        if (user == null) return true;

        // Si permissions vides = acces complet
        if (user.getPermissions() == null || user.getPermissions().isBlank()) return true;

        if (!user.getPermissions().contains(requiredPerm)) {
            response.sendRedirect("/dashboard?access=denied&section=" + requiredPerm.toLowerCase());
            return false;
        }

        return true;
    }
}
