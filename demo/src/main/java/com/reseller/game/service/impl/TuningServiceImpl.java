package com.reseller.game.service.impl;

import com.reseller.game.model.entity.Tuning;
import com.reseller.game.model.entity.types.TuningType;
import com.reseller.game.repository.TuningRepository;
import com.reseller.game.service.TuningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class TuningServiceImpl implements TuningService {

    private final TuningRepository tuningRepository;

    @Override
    @Transactional
    public void initTuning(List<Tuning> tunings) {
        try {
            tuningRepository.saveAllAndFlush(tunings);
        } catch (Exception e) {
            log.error("Error initializing tunings table", e);
            // TODO: Implement customs exception
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tuning> getAllTunings() {
        return tuningRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tuning> getRandomTuning(TuningType type) {
        List<Tuning> filtered = tuningRepository.findByType(type);
        if (filtered.isEmpty()) {
            return Optional.empty();
        }
        int idx = ThreadLocalRandom.current().nextInt(filtered.size());
        return Optional.of(filtered.get(idx));
    }
}
