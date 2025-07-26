package com.reseller.game.repository;

import com.reseller.game.model.entity.types.TuningType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.reseller.game.model.entity.Tuning;

import java.util.List;

@Repository
public interface TuningRepository extends JpaRepository<Tuning, Long> {
    List<Tuning> findByType(TuningType type);
}
