package com.neiverlabs.company.repo;

import com.neiverlabs.company.domain.Company;
import com.neiverlabs.company.domain.CompanyStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

  boolean existsByTaxId(String taxId);

  boolean existsByTaxIdAndIdNot(String taxId, UUID id);

  long countByStatus(CompanyStatus status);

  @Query("""
      select c from Company c
      where lower(c.legalName) like :pattern escape '\\'
         or lower(c.tradeName) like :pattern escape '\\'
         or lower(c.taxId) like :pattern escape '\\'
      """)
  Page<Company> search(@Param("pattern") String pattern, Pageable pageable);
}
