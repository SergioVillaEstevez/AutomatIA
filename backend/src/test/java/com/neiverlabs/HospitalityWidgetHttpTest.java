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
class HospitalityWidgetHttpTest extends AbstractPlatformTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private PlatformFixtures fixtures;

  private String token;

  @BeforeEach
  void setUp() throws Exception {
    fixtures.superAdmin("root-widget@neiver.test", "Root-Admin-12");
    String superToken = login("root-widget@neiver.test", "Root-Admin-12").accessToken();
    String companyId = createCompany(superToken);
    mockMvc.perform(post("/api/v1/admin/companies/" + companyId + "/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + superToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "firstName":"Eva",
                  "lastName":"Sala",
                  "email":"eva-widget@neiver.test",
                  "role":"COMPANY_ADMIN",
                  "password":"Temporal-User-12"
                }
                """))
        .andExpect(status().isCreated());
    token = changePassword(login("eva-widget@neiver.test", "Temporal-User-12"), "Eva-Widget-12").accessToken();
  }

  @Test
  void publicBookingFloorPlanTpvAndTemplates() throws Exception {
    mockMvc.perform(put("/api/v1/company/hospitality/venue")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "publicSlug":"mesa-del-tajo",
                  "publicBookingEnabled":true,
                  "publicBookingNote":"Te confirmaremos por teléfono",
                  "floorWidth":800,
                  "floorHeight":500
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicPath").value("/reservar/mesa-del-tajo"));

    MvcResult table = mockMvc.perform(post("/api/v1/company/hospitality/resources")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"Terraza 1","seats":4,"sortOrder":1,"posX":80,"posY":60,"width":100,"height":80}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.posX").value(80))
        .andReturn();
    String resourceId = com.jayway.jsonpath.JsonPath.read(table.getResponse().getContentAsString(), "$.id");

    mockMvc.perform(get("/api/v1/public/hospitality/mesa-del-tajo"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tradeName").value("Mesa del Tajo"))
        .andExpect(jsonPath("$.resources[0].name").value("Terraza 1"));

    ZonedDateTime start = LocalDate.now(ZoneId.of("Europe/Madrid")).plusDays(2).atTime(20, 30)
        .atZone(ZoneId.of("Europe/Madrid"));
    mockMvc.perform(post("/api/v1/public/hospitality/mesa-del-tajo/reservations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "resourceId":"%s",
                  "guestName":"Cliente Web",
                  "guestPhone":"600999888",
                  "partySize":2,
                  "startsAt":"%s"
                }
                """.formatted(resourceId, start.toInstant())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING"));

    mockMvc.perform(get("/api/v1/company/hospitality/floor-plan")
            .param("day", start.toLocalDate().toString())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resources[0].name").value("Terraza 1"))
        .andExpect(jsonPath("$.openReservations.length()").value(1));

    mockMvc.perform(post("/api/v1/company/hospitality/whatsapp/templates")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code":"confirmacion",
                  "title":"Confirmación",
                  "body":"Hola {{guestName}}, reserva {{startsAt}} para {{partySize}}."
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("confirmacion"));

    MvcResult ticket = mockMvc.perform(post("/api/v1/company/hospitality/tickets")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"resourceId":"%s","label":"Mesa Terraza 1"}
                """.formatted(resourceId)))
        .andExpect(status().isCreated())
        .andReturn();
    String ticketId = com.jayway.jsonpath.JsonPath.read(ticket.getResponse().getContentAsString(), "$.id");

    mockMvc.perform(post("/api/v1/company/hospitality/tickets/" + ticketId + "/lines")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"description":"Menú del día","quantity":2,"unitPriceEur":12.50}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalEur").value(25.00));

    mockMvc.perform(post("/api/v1/company/hospitality/tickets/" + ticketId + "/payment")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"paymentMethod":"CASH"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAID"))
        .andExpect(jsonPath("$.paymentMethod").value("CASH"));
  }

  private String createCompany(String superToken) throws Exception {
    MvcResult company = mockMvc.perform(post("/api/v1/admin/companies")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + superToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "legalName":"Mesa del Tajo SL",
                  "tradeName":"Mesa del Tajo",
                  "taxId":"00000013J",
                  "email":"mesa-tajo@neiver.test",
                  "planLabel":"HOSTELERIA"
                }
                """))
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
    String access = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
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
