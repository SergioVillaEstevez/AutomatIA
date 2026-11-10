package com.neiverlabs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neiverlabs.identity.domain.UserAccount;
import com.neiverlabs.support.AbstractPlatformTest;
import com.neiverlabs.tenant.RlsSession;
import com.neiverlabs.tenant.TenantContext;
import com.neiverlabs.tenant.TenantSnapshot;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

class RowLevelSecurityTest extends AbstractPlatformTest {

  @Autowired
  private DataSource dataSource;
  @Autowired
  private EntityManager entityManager;
  @Autowired
  private RlsSession rlsSession;
  @Autowired
  private TransactionTemplate transactionTemplate;

  @Test
  void queriesWithoutATenantSeeNothingUntilTheSessionSelectsOneCompany() {
    UUID companyId = insertCompany("RLS Uno", "00000008P");
    insertCompany("RLS Dos", "00000009D");

    TenantContext.clear();
    assertThat(countCompanies()).isZero();

    TenantContext.set(TenantSnapshot.company(null, companyId));
    assertThat(countCompanies()).isEqualTo(1);
    TenantContext.clear();
  }

  @Test
  void aUserCannotBeStoredWithoutAMembership() {
    TenantContext.set(TenantSnapshot.bypass(null));
    assertThatThrownBy(() -> transactionTemplate.execute(status -> {
      rlsSession.sync();
      entityManager.persist(new UserAccount("huerfano@neiver.test", "{bcrypt}hash", "Sin", "Empresa"));
      entityManager.flush();
      return null;
    })).hasStackTraceContaining("user_without_membership");
    TenantContext.clear();
  }

  private UUID insertCompany(String name, String taxId) {
    TenantContext.set(TenantSnapshot.bypass(null));
    UUID id = transactionTemplate.execute(status -> {
      rlsSession.sync();
      UUID companyId = UUID.randomUUID();
      entityManager.createNativeQuery("""
              insert into companies (
                id, legal_name, trade_name, tax_id, email, country, status, plan_label, created_at, updated_at
              ) values (
                :id, :name, :name, :taxId, :email, 'ES', 'ACTIVE', 'PILOTO', now(), now()
              )
              """)
          .setParameter("id", companyId)
          .setParameter("name", name)
          .setParameter("taxId", taxId)
          .setParameter("email", taxId.toLowerCase() + "@neiver.test")
          .executeUpdate();
      return companyId;
    });
    TenantContext.clear();
    return id;
  }

  private int countCompanies() {
    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement();
         ResultSet result = statement.executeQuery("select count(*) from companies")) {
      result.next();
      return result.getInt(1);
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }
}
