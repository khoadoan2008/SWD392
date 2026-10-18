package com.aives.repository;

import com.aives.model.entity.Rubric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RubricRepository extends JpaRepository<Rubric, Integer> {

    List<Rubric> findByQuestionIdOrderBySortOrderAscIdAsc(Integer questionId);

    void deleteByQuestionId(Integer questionId);
}
