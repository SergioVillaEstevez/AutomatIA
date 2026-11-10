package com.neiverlabs.identity.repo;

import com.neiverlabs.identity.domain.Membership;
import com.neiverlabs.identity.domain.Role;
import com.neiverlabs.identity.web.UserResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

  Optional<Membership> findByUserId(UUID userId);

  boolean existsByRole(Role role);

  @Query("""
      select new com.neiverlabs.identity.web.UserResponse(
        u.id, u.email, u.firstName, u.lastName, u.status, m.role,
        u.passwordChangeRequired, u.lastLoginAt, m.companyId)
      from UserAccount u, Membership m
      where u.id = m.userId and m.companyId = :companyId
      order by u.lastName asc, u.firstName asc
      """)
  List<UserResponse> findUsersInCompany(@Param("companyId") UUID companyId);
}
