package com.neiverlabs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neiverlabs.support.AbstractPlatformTest;
import com.neiverlabs.support.PlatformFixtures;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Import(PlatformFixtures.class)
class PlatformHttpTest extends AbstractPlatformTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private PlatformFixtures fixtures;

  private String superToken;

  @BeforeEach
  void seedSuperAdmin() throws Exception {
    fixtures.superAdmin("root@neiver.test", "Root-Admin-12");
    Session session = login("root@neiver.test", "Root-Admin-12");
    superToken = session.accessToken();
  }

  @Test
  void loginDoesNotExposeTheRefreshTokenAndMeReturnsThePlatformRole() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"root@neiver.test","password":"Root-Admin-12"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.refreshToken").doesNotExist())
        .andExpect(jsonPath("$.user.role").value("SUPER_ADMIN"))
        .andReturn();

    String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
    assertThat(setCookie).contains("neiver_refresh=");
    assertThat(setCookie.toLowerCase()).contains("httponly");
    assertThat(setCookie.toLowerCase()).contains("samesite=strict");
    assertThat(result.getResponse().getContentAsString()).doesNotContain(cookieValue(setCookie));

    mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(superToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("root@neiver.test"))
        .andExpect(jsonPath("$.companyId").doesNotExist());
  }

  @Test
  void unknownPasswordDoesNotRevealWhetherTheAccountExists() throws Exception {
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"nadie@neiver.test","password":"Root-Admin-12"}
                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  }

  @Test
  void companiesStayIsolatedAndCompanyUsersCannotAdministerThePlatform() throws Exception {
    String martinezId = createCompany("Transporte Martínez SL", "Transporte Martínez", "00000001R",
        "contacto-martinez@neiver.test", "10.00");
    String toledoId = createCompany("Almacenes Toledo SL", "Almacenes Toledo", "00000002W",
        "contacto-toledo@neiver.test", "50.00");
    createUser(martinezId, "ana@martinez.test", "COMPANY_ADMIN", "Ana", "Martínez");
    createUser(toledoId, "luis@toledo.test", "COMPANY_ADMIN", "Luis", "Toledo");
    createUser(martinezId, "nora@martinez.test", "COMPANY_USER", "Nora", "Sanz");

    Session ana = changePassword(login("ana@martinez.test", "Temporal-User-12"), "Clave-Ana-Martinez-12");
    Session luis = changePassword(login("luis@toledo.test", "Temporal-User-12"), "Clave-Luis-Toledo-12");
    Session nora = changePassword(login("nora@martinez.test", "Temporal-User-12"), "Clave-Nora-Sanz-12");

    mockMvc.perform(get("/api/v1/admin/companies").header(HttpHeaders.AUTHORIZATION, bearer(ana.accessToken())))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/v1/company").header(HttpHeaders.AUTHORIZATION, bearer(ana.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tradeName").value("Transporte Martínez"))
        .andExpect(jsonPath("$.aiBudgetEnforced").value(true))
        .andExpect(jsonPath("$.monthlyAiBudgetEur").value(10.00));

    mockMvc.perform(get("/api/v1/company/users").header(HttpHeaders.AUTHORIZATION, bearer(ana.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.email == 'ana@martinez.test')]").exists())
        .andExpect(jsonPath("$[?(@.email == 'luis@toledo.test')]").doesNotExist());

    mockMvc.perform(put("/api/v1/company/users/" + userId(toledoId, "luis@toledo.test"))
            .header(HttpHeaders.AUTHORIZATION, bearer(ana.accessToken()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"luis@toledo.test","firstName":"Luis","lastName":"Robado","role":"COMPANY_USER"}
                """))
        .andExpect(status().isNotFound());

    mockMvc.perform(get("/api/v1/company/users").header(HttpHeaders.AUTHORIZATION, bearer(nora.accessToken())))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/v1/company/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(nora.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.company.id").value(martinezId))
        .andExpect(jsonPath("$.activeUsers").value(2));

    mockMvc.perform(get("/api/v1/company/audit").header(HttpHeaders.AUTHORIZATION, bearer(luis.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[?(@.action == 'USER_CREATED' && @.details.role == 'COMPANY_ADMIN')]").exists())
        .andExpect(jsonPath("$.items[?(@.details.email == 'ana@martinez.test')]").doesNotExist());

    String audit = mockMvc.perform(get("/api/v1/admin/audit").header(HttpHeaders.AUTHORIZATION, bearer(superToken)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    assertThat(audit).doesNotContain("Temporal-User-12");
    assertThat(audit).doesNotContain("Clave-Ana-Martinez-12");
  }

  @Test
  void passwordChangeIsRequiredBeforeUsingTheCompany() throws Exception {
    String companyId = createCompany("Distribuciones La Sagra SL", "Distribuciones La Sagra", "00000003A",
        "contacto-sagra@neiver.test", null);
    createUser(companyId, "marta@sagra.test", "COMPANY_ADMIN", "Marta", "Sagra");
    Session pending = login("marta@sagra.test", "Temporal-User-12");

    mockMvc.perform(get("/api/v1/company/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(pending.accessToken())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));

    Session ready = changePassword(pending, "Clave-Marta-Sagra-12");
    mockMvc.perform(get("/api/v1/company").header(HttpHeaders.AUTHORIZATION, bearer(ready.accessToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tradeName").value("Distribuciones La Sagra"));
  }

  @Test
  void repeatedLoginFailuresLockTheAccount() throws Exception {
    String companyId = createCompany("Logística Norte SL", "Logística Norte", "00000004G",
        "contacto-norte@neiver.test", null);
    createUser(companyId, "pablo@norte.test", "COMPANY_USER", "Pablo", "Norte");
    for (int attempt = 0; attempt < 3; attempt++) {
      mockMvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {"email":"pablo@norte.test","password":"clave-incorrecta"}
                  """))
          .andExpect(status().isUnauthorized());
    }
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"pablo@norte.test","password":"Temporal-User-12"}
                """))
        .andExpect(status().isLocked())
        .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
  }

  @Test
  void refreshRotationDetectsReuseAndLogoutRevokesTheAccessToken() throws Exception {
    Session first = login("root@neiver.test", "Root-Admin-12");
    MvcResult rotated = mockMvc.perform(post("/api/v1/auth/refresh")
            .cookie(new Cookie("neiver_refresh", first.refreshCookie())))
        .andExpect(status().isOk())
        .andReturn();
    String replacement = cookieValue(rotated.getResponse().getHeader(HttpHeaders.SET_COOKIE));

    mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("neiver_refresh", first.refreshCookie())))
        .andExpect(status().isUnauthorized());
    mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("neiver_refresh", replacement)))
        .andExpect(status().isUnauthorized());

    Session current = login("root@neiver.test", "Root-Admin-12");
    mockMvc.perform(post("/api/v1/auth/logout")
            .cookie(new Cookie("neiver_refresh", current.refreshCookie())))
        .andExpect(status().isNoContent());
    mockMvc.perform(get("/api/v1/admin/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(current.accessToken())))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
  }

  @Test
  void suspendingACompanyCutsOffExistingSessions() throws Exception {
    String companyId = createCompany("Industrial del Tajo SL", "Industrial del Tajo", "00000005M",
        "contacto-tajo@neiver.test", null);
    createUser(companyId, "ines@tajo.test", "COMPANY_ADMIN", "Inés", "Tajo");
    Session ines = changePassword(login("ines@tajo.test", "Temporal-User-12"), "Clave-Ines-Tajo-12");

    mockMvc.perform(post("/api/v1/admin/companies/" + companyId + "/suspension")
            .header(HttpHeaders.AUTHORIZATION, bearer(superToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"reason":"Impago del piloto"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUSPENDED"));

    mockMvc.perform(get("/api/v1/company").header(HttpHeaders.AUTHORIZATION, bearer(ines.accessToken())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("COMPANY_SUSPENDED"));
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"ines@tajo.test","password":"Clave-Ines-Tajo-12"}
                """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("COMPANY_SUSPENDED"));
  }

  @Test
  void theLastCompanyAdminCannotBeRemovedAndContactDataDoesNotChangeTheTaxId() throws Exception {
    String companyId = createCompany("Mensajería Sagra SL", "Mensajería Sagra", "00000006Y",
        "contacto-mensajeria@neiver.test", null);
    createUser(companyId, "eva@mensajeria.test", "COMPANY_ADMIN", "Eva", "Sanz");
    Session eva = changePassword(login("eva@mensajeria.test", "Temporal-User-12"), "Clave-Eva-Sanz-12");
    String evaId = userId(companyId, "eva@mensajeria.test");

    mockMvc.perform(post("/api/v1/admin/companies/" + companyId + "/users/" + evaId + "/deactivation")
            .header(HttpHeaders.AUTHORIZATION, bearer(superToken)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("LAST_ADMIN"));

    mockMvc.perform(put("/api/v1/company")
            .header(HttpHeaders.AUTHORIZATION, bearer(eva.accessToken()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"phone":"925111222","city":"Illescas","province":"Toledo","postalCode":"45200"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.taxId").value("00000006Y"))
        .andExpect(jsonPath("$.city").value("Illescas"))
        .andExpect(jsonPath("$.legalName").value("Mensajería Sagra SL"));
  }

  @Test
  void invalidTaxIdIsRejectedAndSearchDoesNotTreatPercentAsAWildcard() throws Exception {
    createCompany("Transportes del Valle SL", "Transportes del Valle", "B12345674",
        "contacto-valle@neiver.test", null);
    mockMvc.perform(post("/api/v1/admin/companies")
            .header(HttpHeaders.AUTHORIZATION, bearer(superToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"legalName":"Falsa SL","tradeName":"Falsa","taxId":"B1234567J","email":"falsa@neiver.test","planLabel":"PILOTO"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    mockMvc.perform(get("/api/v1/admin/companies").param("q", "%")
            .header(HttpHeaders.AUTHORIZATION, bearer(superToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalItems").value(0));
  }

  @Test
  void adminDashboardCountsRealCompanies() throws Exception {
    createCompany("Harinera de La Sagra SL", "Harinera de La Sagra", "00000007F",
        "contacto-harina@neiver.test", null);
    mockMvc.perform(get("/api/v1/admin/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(superToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.companies").isNumber())
        .andExpect(jsonPath("$.users").isNumber())
        .andExpect(jsonPath("$.recentActivity").isArray());
  }

  private String createCompany(
      String legalName,
      String tradeName,
      String taxId,
      String email,
      String budget
  ) throws Exception {
    String body = """
        {"legalName":"%s","tradeName":"%s","taxId":"%s","email":"%s","city":"Toledo","province":"Toledo","planLabel":"PILOTO","monthlyAiBudgetEur":%s}
        """.formatted(legalName, tradeName, taxId, email, budget == null ? "null" : budget);
    MvcResult result = mockMvc.perform(post("/api/v1/admin/companies")
            .header(HttpHeaders.AUTHORIZATION, bearer(superToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
  }

  private void createUser(String companyId, String email, String role, String firstName, String lastName)
      throws Exception {
    mockMvc.perform(post("/api/v1/admin/companies/" + companyId + "/users")
            .header(HttpHeaders.AUTHORIZATION, bearer(superToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"%s","password":"Temporal-User-12","firstName":"%s","lastName":"%s","role":"%s"}
                """.formatted(email, firstName, lastName, role)))
        .andExpect(status().isCreated());
  }

  private String userId(String companyId, String email) throws Exception {
    MvcResult result = mockMvc.perform(get("/api/v1/admin/companies/" + companyId + "/users")
            .header(HttpHeaders.AUTHORIZATION, bearer(superToken)))
        .andExpect(status().isOk())
        .andReturn();
    JsonNode users = objectMapper.readTree(result.getResponse().getContentAsString());
    for (JsonNode user : users) {
      if (email.equals(user.get("email").asText())) {
        return user.get("id").asText();
      }
    }
    throw new AssertionError("No está " + email);
  }

  private Session login(String email, String password) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"%s","password":"%s"}
                """.formatted(email, password)))
        .andExpect(status().isOk())
        .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    return new Session(
        body.get("accessToken").asText(),
        cookieValue(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)));
  }

  private Session changePassword(Session current, String newPassword) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/auth/change-password")
            .header(HttpHeaders.AUTHORIZATION, bearer(current.accessToken()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"currentPassword":"Temporal-User-12","newPassword":"%s"}
                """.formatted(newPassword)))
        .andExpect(status().isOk())
        .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    return new Session(
        body.get("accessToken").asText(),
        cookieValue(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)));
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static String cookieValue(String setCookie) {
    int start = setCookie.indexOf('=');
    int end = setCookie.indexOf(';');
    return setCookie.substring(start + 1, end);
  }

  private record Session(String accessToken, String refreshCookie) {
  }
}
