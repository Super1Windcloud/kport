package com.example.demo.web;

import com.example.demo.service.ProcessKillService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.superwindcloud.fkill.FkillException;

@RestController
@RequestMapping("/api/processes")
public class ProcessKillController {
  private final ProcessKillService processKillService;

  public ProcessKillController(ProcessKillService processKillService) {
    this.processKillService = processKillService;
  }

  @PostMapping("/kill")
  public ResponseEntity<ApiResponse<Map<String, Object>>> kill(
      @RequestHeader(name = "X-Admin-Token", required = false) String adminToken,
      @Valid @RequestBody KillRequest request)
      throws FkillException {
    processKillService.validateToken(adminToken);
    processKillService.kill(request);

    return ResponseEntity.ok(
        ApiResponse.success(
            "Processes terminated.",
            Map.of(
                "targets", request.targets(),
                "tree", request.tree() == null || request.tree())));
  }
}
