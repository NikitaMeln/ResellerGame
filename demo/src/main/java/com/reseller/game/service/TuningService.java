package com.reseller.game.service;

import com.reseller.game.model.entity.Tuning;
import com.reseller.game.model.entity.types.TuningType;

import java.util.List;
import java.util.Optional;

public interface TuningService {

    void initTuning(List<Tuning> tunings);

    List<Tuning> getAllTunings();

    Optional<Tuning> getRandomTuning(TuningType type);
}
