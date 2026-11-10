package com.neiverlabs.identity.repo;

import com.neiverlabs.identity.domain.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("""
      update RefreshToken t
      set t.revokedAt = :now
      where t.revokedAt is null and t.userId = :userId
      """)
  int revokeActiveForUser(@Param("userId") UUID userId, @Param("now") Instant now);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("""
      update RefreshToken t
      set t.revokedAt = :now
      where t.revokedAt is null and t.familyId = :familyId
      """)
  int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("""
      update RefreshToken t
      set t.revokedAt = :now
      where t.revokedAt is null and t.userId in (
        select m.userId from Membership m where m.companyId = :companyId
      )
      """)
  int revokeActiveForCompany(@Param("companyId") UUID companyId, @Param("now") Instant now);
}
