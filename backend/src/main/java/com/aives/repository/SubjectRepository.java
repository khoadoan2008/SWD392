package com.aives.repository;

import com.aives.model.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subject s WHERE s.id = :id")
    Optional<Subject> lockById(@Param("id") Integer id);

    Optional<Subject> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Subject> findByActiveTrueOrderByCodeAsc();

    @Query("SELECT s FROM User u JOIN u.assignedSubjects s WHERE u.id = :lecturerId ORDER BY s.code ASC")
    List<Subject> findByLecturerId(@Param("lecturerId") Integer lecturerId);
}
