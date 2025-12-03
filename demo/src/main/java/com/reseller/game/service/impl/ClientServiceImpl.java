package com.reseller.game.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.reseller.game.model.entity.Client;
import com.reseller.game.repository.ClientRepository;
import com.reseller.game.service.ClientService;
import com.reseller.game.util.DataInitializationUtil;
import com.reseller.game.util.RandomSelectionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    @Override
    public void initClients(List<Client> clients) {
        DataInitializationUtil.initializeData(clientRepository, clients, "clients");
    }

    @Override
    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    @Override
    public Client getRandomClient(List<Client> clients) {
        return RandomSelectionUtil.selectRandom(clients).orElse(null);
    }
}
