package com.neiverlabs.ai.web;

import com.neiverlabs.ai.service.AiService;
import com.neiverlabs.shared.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/company/ai")
public class CompanyAiController {

  private final AiService ai;

  public CompanyAiController(AiService ai) {
    this.ai = ai;
  }

  @PostMapping("/completions")
  @ResponseStatus(HttpStatus.OK)
  public AiCompletionResponse complete(@Valid @RequestBody CompleteAiRequest request) {
    return ai.complete(request);
  }

  @GetMapping("/usage")
  public PageResponse<AiUsageResponse> usage(Pageable pageable) {
    return ai.companyUsage(pageable);
  }

  @GetMapping("/usage/summary")
  public AiUsageSummaryResponse summary() {
    return ai.companySummary();
  }
}
