package com.reseller.game.service;

import java.util.List;

import com.reseller.game.model.entity.Client;

public interface ClientService {

    void initClients(List<Client> clients);

    List<Client> getAllClients();

    Client getRandomClient(List<Client> clients);
}
