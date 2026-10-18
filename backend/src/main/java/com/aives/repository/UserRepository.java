package com.aives.repository;

import com.aives.model.entity.User;
import com.aives.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:q IS NULL OR :q = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.studentCode) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY u.id DESC")
    List<User> searchUsers(@Param("role") Role role, @Param("q") String q);

    long countByRole(Role role);
}
