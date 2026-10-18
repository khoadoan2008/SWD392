package com.aives.repository;

import com.aives.model.entity.InterviewSession;
import com.aives.model.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Integer> {

    Optional<InterviewSession> findFirstByStudentIdAndSubjectIdAndStatusOrderByIdDesc(
            Integer studentId, Integer subjectId, SessionStatus status);

    List<InterviewSession> findByStudentIdOrderByIdDesc(Integer studentId);

    @Query("SELECT s FROM InterviewSession s WHERE " +
           "(:studentId IS NULL OR s.student.id = :studentId) AND " +
           "(:allowedSubjectIds IS NULL OR s.subject.id IN :allowedSubjectIds) AND " +
           "(:subjectId IS NULL OR s.subject.id = :subjectId) " +
           "ORDER BY s.id DESC")
    List<InterviewSession> search(
            @Param("studentId") Integer studentId,
            @Param("allowedSubjectIds") List<Integer> allowedSubjectIds,
            @Param("subjectId") Integer subjectId
    );

    @Query("SELECT s.status, COUNT(s) FROM InterviewSession s WHERE " +
           "(:allowedSubjectIds IS NULL OR s.subject.id IN :allowedSubjectIds) " +
           "GROUP BY s.status")
    List<Object[]> countByStatus(@Param("allowedSubjectIds") List<Integer> allowedSubjectIds);
}
