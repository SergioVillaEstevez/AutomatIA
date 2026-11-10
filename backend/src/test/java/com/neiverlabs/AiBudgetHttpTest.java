package com.neiverlabs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.neiverlabs.support.AbstractPlatformTest;
import com.neiverlabs.support.PlatformFixtures;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Import(PlatformFixtures.class)
class AiBudgetHttpTest extends AbstractPlatformTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private PlatformFixtures fixtures;

  private String companyToken;

  @BeforeEach
  void setUp() throws Exception {
    fixtures.superAdmin("root-ai@neiver.test", "Root-Admin-12");
    String superToken = login("root-ai@neiver.test", "Root-Admin-12").accessToken();
    MvcResult company = mockMvc.perform(post("/api/v1/admin/companies")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + superToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "legalName":"Almacenes Prueba SL",
                  "tradeName":"Almacenes Prueba",
                  "taxId":"00000010X",
                  "email":"almacen@neiver.test",
                  "planLabel":"PILOTO",
                  "monthlyAiBudgetEur":0
                }
                """))
        .andExpect(status().isCreated())
        .andReturn();
    String companyId = com.jayway.jsonpath.JsonPath.read(company.getResponse().getContentAsString(), "$.id");
    mockMvc.perform(post("/api/v1/admin/companies/" + companyId + "/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + superToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "firstName":"Luis",
                  "lastName":"Prueba",
                  "email":"luis-ai@neiver.test",
                  "role":"COMPANY_ADMIN",
                  "password":"Temporal-User-12"
                }
                """))
        .andExpect(status().isCreated());
    companyToken = changePassword(
        login("luis-ai@neiver.test", "Temporal-User-12"),
        "Luis-Prueba-AI-12").accessToken();
  }

  @Test
  void blocksCompletionWhenMonthlyBudgetIsZero() throws Exception {
    mockMvc.perform(post("/api/v1/company/ai/completions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + companyToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"prompt":"Hola","taskType":"DEFAULT"}
                """))
        .andExpect(status().isPaymentRequired())
        .andExpect(jsonPath("$.code").value("AI_BUDGET_EXCEEDED"));
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
