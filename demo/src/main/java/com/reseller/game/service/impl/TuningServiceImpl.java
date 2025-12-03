package com.reseller.game.service.impl;

import com.reseller.game.model.entity.Tuning;
import com.reseller.game.model.entity.types.TuningType;
import com.reseller.game.repository.TuningRepository;
import com.reseller.game.service.TuningService;
import com.reseller.game.util.DataInitializationUtil;
import com.reseller.game.util.RandomSelectionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TuningServiceImpl implements TuningService {

    private final TuningRepository tuningRepository;

    @Override
    @Transactional
    public void initTuning(List<Tuning> tunings) {
        DataInitializationUtil.initializeData(tuningRepository, tunings, "tunings", true, true);
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
        return RandomSelectionUtil.selectRandom(filtered);
    }

    @Transactional(readOnly = true)
    public List<Tuning> getTuningsByType(TuningType type) {
        return tuningRepository.findByType(type);
    }
}
