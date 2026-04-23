package com.example.demo.web;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record KillRequest(
    @NotEmpty(message = "targets must not be empty") List<String> targets,
    Boolean ignoreCase,
    Boolean tree,
    Boolean silent,
    Long forceAfterTimeoutMs,
    Long waitForExitMs) {}
