package com.neiverlabs.identity.repo;

import com.neiverlabs.identity.domain.UserAccount;
import com.neiverlabs.identity.domain.UserStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

  Optional<UserAccount> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByEmailAndIdNot(String email, UUID id);

  long countByStatus(UserStatus status);

  @Query("""
      select count(u) from UserAccount u, Membership m
      where u.id = m.userId
        and m.companyId = :companyId
        and m.role = com.neiverlabs.identity.domain.Role.COMPANY_ADMIN
        and u.status = com.neiverlabs.identity.domain.UserStatus.ACTIVE
        and u.id <> :userId
      """)
  long countOtherActiveAdmins(@Param("companyId") UUID companyId, @Param("userId") UUID userId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("""
      update UserAccount u
      set u.sessionVersion = u.sessionVersion + 1
      where u.id in (select m.userId from Membership m where m.companyId = :companyId)
      """)
  int bumpSessionsForCompany(@Param("companyId") UUID companyId);

  @Query("""
      select count(u) from UserAccount u, Membership m
      where u.id = m.userId and m.companyId = :companyId and u.status = :status
      """)
  long countInCompany(@Param("companyId") UUID companyId, @Param("status") UserStatus status);
}
