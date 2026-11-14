package com.library.member;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "memberType")
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "memberType")
    Optional<User> findByCardNumber(String cardNumber);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    long countByRoleAndCreatedAtBetween(Role role, LocalDateTime from, LocalDateTime to);

    List<User> findByRoleInOrderByNameAsc(List<Role> roles);

    /** Members matching a name, email or card number, optionally filtered by status and type. */
    @EntityGraph(attributePaths = "memberType")
    @Query("""
            SELECT u FROM User u
            WHERE u.role = com.library.member.Role.MEMBER
              AND (:q IS NULL
                   OR LOWER(u.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.cardNumber) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:status IS NULL OR u.status = :status)
              AND (:typeId IS NULL OR u.memberType.id = :typeId)
            """)
    Page<User> searchMembers(@Param("q") String q, @Param("status") AccountStatus status,
                             @Param("typeId") Long typeId, Pageable pageable);
}
