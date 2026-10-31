package com.aives.repository;

import com.aives.model.entity.SessionRubricScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRubricScoreRepository extends JpaRepository<SessionRubricScore, Integer> {
    List<SessionRubricScore> findBySessionId(Integer sessionId);
    List<SessionRubricScore> findBySessionIdOrderByIdAsc(Integer sessionId);
    void deleteBySessionId(Integer sessionId);
}
