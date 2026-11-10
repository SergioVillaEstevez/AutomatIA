package com.neiverlabs.hospitality.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospitality_whatsapp_templates")
public class HospitalityWhatsAppTemplate {

  @Id
  private UUID id;

  @Column(name = "company_id", nullable = false)
  private UUID companyId;

  @Column(nullable = false, length = 60)
  private String code;

  @Column(nullable = false, length = 120)
  private String title;

  @Column(nullable = false, length = 1000)
  private String body;

  @Column(name = "meta_template_name", length = 120)
  private String metaTemplateName;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected HospitalityWhatsAppTemplate() {
  }

  public HospitalityWhatsAppTemplate(
      UUID companyId, String code, String title, String body, String metaTemplateName
  ) {
    this.id = UUID.randomUUID();
    this.companyId = companyId;
    this.code = code;
    this.title = title;
    this.body = body;
    this.metaTemplateName = metaTemplateName;
    this.active = true;
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getCompanyId() {
    return companyId;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getMetaTemplateName() {
    return metaTemplateName;
  }

  public void setMetaTemplateName(String metaTemplateName) {
    this.metaTemplateName = metaTemplateName;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
