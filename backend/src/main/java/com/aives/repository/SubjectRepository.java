package com.aives.repository;

import com.aives.model.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Integer> {

    Optional<Subject> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Subject> findByActiveTrueOrderByCodeAsc();

    @Query("SELECT s FROM Subject s JOIN User u JOIN u.assignedSubjects sub WHERE u.id = :lecturerId AND sub.id = s.id ORDER BY s.code ASC")
    List<Subject> findByLecturerId(@Param("lecturerId") Integer lecturerId);
}
