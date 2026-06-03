package com.yuan.daydayup.auth.controller;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.Set;

@Controller
public class ConsentController {

    private final RegisteredClientRepository clientRepository;

    public ConsentController(RegisteredClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @GetMapping("/oauth2/consent")
    public String consent(Principal principal,
                          Model model,
                          @RequestParam("client_id") String clientId,
                          @RequestParam("scope") String scope,
                          @RequestParam("state") String state) {

        RegisteredClient client = clientRepository.findByClientId(clientId);
        if (client == null) {
            throw new IllegalArgumentException("Unknown client: " + clientId);
        }

        Set<String> scopesToApprove = new LinkedHashSet<>();
        for (String s : scope.split(" ")) {
            if (StringUtils.hasText(s)) {
                scopesToApprove.add(s);
            }
        }

        model.addAttribute("clientId", clientId);
        model.addAttribute("clientName", client.getClientName());
        model.addAttribute("state", state);
        model.addAttribute("scopes", scopesToApprove);

        return "consent";
    }
}
