package com.reseller.game.service.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.reseller.game.model.entity.Tuning;
import org.springframework.stereotype.Service;

import com.reseller.game.model.entity.Client;
import com.reseller.game.repository.ClientRepository;

@Service
public class ClientServiceImpl {

    private final ClientRepository clientRepository;

    public ClientServiceImpl(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public void initClients(List<Client> clients) {
        try {
            clientRepository.saveAll(clients);
        } catch (Exception e) {
            System.err.println("Error initializing clients: " + e.getMessage());
        }
    }

    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    public Client getRandomClient(List<Client> clients) {
        if (clients == null || clients.isEmpty()) {
            return null;
        }
        int randomIndex = (int) (Math.random() * clients.size());
        return clients.get(randomIndex);
    }
}
