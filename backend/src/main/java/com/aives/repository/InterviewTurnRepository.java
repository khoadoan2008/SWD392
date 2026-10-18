package com.aives.repository;

import com.aives.model.entity.InterviewTurn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewTurnRepository extends JpaRepository<InterviewTurn, Integer> {

    List<InterviewTurn> findBySessionIdOrderByMainIndexAscFollowupIndexAsc(Integer sessionId);

    @Query("SELECT t FROM InterviewTurn t WHERE t.session.id = :sessionId AND t.answeredAt IS NULL ORDER BY t.mainIndex ASC, t.followupIndex ASC")
    List<InterviewTurn> findUnansweredTurns(@Param("sessionId") Integer sessionId);

    default Optional<InterviewTurn> findCurrentTurn(Integer sessionId) {
        List<InterviewTurn> list = findUnansweredTurns(sessionId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
