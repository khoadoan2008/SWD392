package com.aives.repository;

import com.aives.model.entity.Question;
import com.aives.model.enums.BloomLevel;
import com.aives.model.enums.QuestionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {

    @Query("SELECT q FROM Question q WHERE " +
           "(:allowedSubjectIds IS NULL OR q.subject.id IN :allowedSubjectIds) AND " +
           "(:subjectId IS NULL OR q.subject.id = :subjectId) AND " +
           "(:status IS NULL OR q.status = :status) AND " +
           "(:bloomLevel IS NULL OR q.bloomLevel = :bloomLevel) AND " +
           "(:kw IS NULL OR :kw = '' OR LOWER(q.content) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(q.topic) LIKE LOWER(CONCAT('%', :kw, '%'))) " +
           "ORDER BY q.id DESC")
    List<Question> search(
            @Param("allowedSubjectIds") List<Integer> allowedSubjectIds,
            @Param("subjectId") Integer subjectId,
            @Param("status") QuestionStatus status,
            @Param("bloomLevel") BloomLevel bloomLevel,
            @Param("kw") String kw
    );

    @Query("SELECT q.status, COUNT(q) FROM Question q WHERE " +
           "(:allowedSubjectIds IS NULL OR q.subject.id IN :allowedSubjectIds) " +
           "GROUP BY q.status")
    List<Object[]> countByStatus(@Param("allowedSubjectIds") List<Integer> allowedSubjectIds);

    @Query("SELECT q FROM Question q WHERE q.subject.id = :subjectId AND q.status = com.aives.model.enums.QuestionStatus.APPROVED ORDER BY FUNCTION('RANDOM')")
    List<Question> findRandomApprovedBySubject(@Param("subjectId") Integer subjectId, Pageable pageable);

    List<Question> findBySubjectIdAndStatusOrderByIdDesc(Integer subjectId, QuestionStatus status);
}
