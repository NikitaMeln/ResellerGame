package com.reseller.game.service;

import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;
import com.reseller.game.model.entity.Tuning;
import com.reseller.game.model.entity.types.TuningType;
import com.reseller.game.repository.TuningRepository;

@Service
public class TuningService {

    private final Random random = new Random();

    private final TuningRepository tuningRepository;

    public TuningService(TuningRepository tuningRepository) {
        this.tuningRepository = tuningRepository;
    }

    public void initTuning(List<Tuning> tunings) {
        try {
            tuningRepository.saveAllAndFlush(tunings);
        } catch (Exception e) {
            System.err.println("Error initializing tunings table: " + e.getMessage());
        }
    }

    public List<Tuning> getAllTunnings() {
        return tuningRepository.findAll();
    }

    private Tuning getRandomTuning(List<Tuning> tunings, TuningType type) {
        List<Tuning> filtered = tunings.stream()
            .filter(t -> t.getType() == type)
            .toList();
        return filtered.get(random.nextInt(filtered.size()));
    }
}
