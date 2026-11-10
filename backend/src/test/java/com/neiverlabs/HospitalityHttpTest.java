package com.neiverlabs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.neiverlabs.support.AbstractPlatformTest;
import com.neiverlabs.support.PlatformFixtures;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Import(PlatformFixtures.class)
class HospitalityHttpTest extends AbstractPlatformTest {

  private static final ZoneId ZONE = ZoneId.of("Europe/Madrid");

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private PlatformFixtures fixtures;

  private String companyToken;
  private String otherToken;

  @BeforeEach
  void setUp() throws Exception {
    fixtures.superAdmin("root-hosp@neiver.test", "Root-Admin-12");
    String superToken = login("root-hosp@neiver.test", "Root-Admin-12").accessToken();

    String companyA = createCompany(superToken, "Restaurante Sagra SL", "Sagra Mesa", "00000011B",
        "sagra-mesa@neiver.test");
    mockMvc.perform(post("/api/v1/admin/companies/" + companyA + "/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + superToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "firstName":"Ana",
                  "lastName":"Hostelería",
                  "email":"ana-hosp@neiver.test",
                  "role":"COMPANY_ADMIN",
                  "password":"Temporal-User-12"
                }
                """))
        .andExpect(status().isCreated());
    companyToken = changePassword(login("ana-hosp@neiver.test", "Temporal-User-12"), "Ana-Hosp-AI-12").accessToken();

    String companyB = createCompany(superToken, "Bar Tajo SL", "Bar Tajo", "00000012N", "bar-tajo@neiver.test");
    mockMvc.perform(post("/api/v1/admin/companies/" + companyB + "/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + superToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "firstName":"Paco",
                  "lastName":"Tajo",
                  "email":"paco-hosp@neiver.test",
                  "role":"COMPANY_ADMIN",
                  "password":"Temporal-User-12"
                }
                """))
        .andExpect(status().isCreated());
    otherToken = changePassword(login("paco-hosp@neiver.test", "Temporal-User-12"), "Paco-Hosp-AI-12").accessToken();
  }

  @Test
  void managesResourcesReservationsOccupancyAndBlocksOverlap() throws Exception {
    MvcResult table = mockMvc.perform(post("/api/v1/company/hospitality/resources")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + companyToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"Mesa 1","seats":4,"sortOrder":1}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Mesa 1"))
        .andReturn();
    String resourceId = com.jayway.jsonpath.JsonPath.read(table.getResponse().getContentAsString(), "$.id");

    ZonedDateTime start = LocalDate.now(ZONE).plusDays(1).atTime(21, 0).atZone(ZONE);
    String startsAt = start.toInstant().toString();
    String endsAt = start.plusMinutes(90).toInstant().toString();

    MvcResult reservation = mockMvc.perform(post("/api/v1/company/hospitality/reservations")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + companyToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "resourceId":"%s",
                  "guestName":"Laura Pérez",
                  "guestPhone":"600111222",
                  "partySize":2,
                  "startsAt":"%s",
                  "endsAt":"%s",
                  "status":"CONFIRMED"
                }
                """.formatted(resourceId, startsAt, endsAt)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.guestName").value("Laura Pérez"))
        .andExpect(jsonPath("$.resourceName").value("Mesa 1"))
        .andReturn();
    String reservationId = com.jayway.jsonpath.JsonPath.read(
        reservation.getResponse().getContentAsString(), "$.id");

    mockMvc.perform(post("/api/v1/company/hospitality/reservations")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + companyToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "resourceId":"%s",
                  "guestName":"Otro",
                  "partySize":2,
                  "startsAt":"%s",
                  "endsAt":"%s"
                }
                """.formatted(resourceId, startsAt, endsAt)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

    mockMvc.perform(get("/api/v1/company/hospitality/occupancy")
            .param("day", start.toLocalDate().toString())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + companyToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.activeSeats").value(4))
        .andExpect(jsonPath("$.reservedCovers").value(2))
        .andExpect(jsonPath("$.openReservations").value(1));

    mockMvc.perform(get("/api/v1/company/hospitality/reservations")
            .param("day", start.toLocalDate().toString())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalItems").value(0));

    mockMvc.perform(post("/api/v1/company/hospitality/reservations/" + reservationId + "/whatsapp")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + companyToken))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("WHATSAPP_NOT_CONFIGURED"));

    mockMvc.perform(put("/api/v1/company/hospitality/reservations/" + reservationId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + companyToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "resourceId":"%s",
                  "guestName":"Laura Pérez",
                  "guestPhone":"600111222",
                  "partySize":2,
                  "startsAt":"%s",
                  "endsAt":"%s",
                  "status":"CANCELLED"
                }
                """.formatted(resourceId, startsAt, endsAt)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  private String createCompany(String superToken, String legal, String trade, String taxId, String email)
      throws Exception {
    MvcResult company = mockMvc.perform(post("/api/v1/admin/companies")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + superToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "legalName":"%s",
                  "tradeName":"%s",
                  "taxId":"%s",
                  "email":"%s",
                  "planLabel":"HOSTELERIA",
                  "monthlyAiBudgetEur":20
                }
                """.formatted(legal, trade, taxId, email)))
        .andExpect(status().isCreated())
        .andReturn();
    return com.jayway.jsonpath.JsonPath.read(company.getResponse().getContentAsString(), "$.id");
  }

  private Session login(String email, String password) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"%s","password":"%s"}
                """.formatted(email, password)))
        .andExpect(status().isOk())
        .andReturn();
    String body = result.getResponse().getContentAsString();
    String access = com.jayway.jsonpath.JsonPath.read(body, "$.accessToken");
    Cookie cookie = result.getResponse().getCookie("neiver_refresh");
    return new Session(access, cookie == null ? null : cookie.getValue());
  }

  private Session changePassword(Session session, String next) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/auth/change-password")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"currentPassword":"Temporal-User-12","newPassword":"%s"}
                """.formatted(next)))
        .andExpect(status().isOk())
        .andReturn();
    String access = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    return new Session(access, session.refreshCookie());
  }

  private record Session(String accessToken, String refreshCookie) {
  }
}
