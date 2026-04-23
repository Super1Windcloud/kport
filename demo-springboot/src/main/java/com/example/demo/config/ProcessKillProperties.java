package com.example.demo.config;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "process-kill")
public class ProcessKillProperties {
  private String apiToken = "change-me";
  private boolean allowPid;
  private Set<Integer> allowedPorts = new LinkedHashSet<>();
  private Set<String> allowedProcessNames = new LinkedHashSet<>();

  public String getApiToken() {
    return apiToken;
  }

  public void setApiToken(String apiToken) {
    this.apiToken = apiToken;
  }

  public boolean isAllowPid() {
    return allowPid;
  }

  public void setAllowPid(boolean allowPid) {
    this.allowPid = allowPid;
  }

  public Set<Integer> getAllowedPorts() {
    return allowedPorts;
  }

  public void setAllowedPorts(Set<Integer> allowedPorts) {
    this.allowedPorts = allowedPorts;
  }

  public Set<String> getAllowedProcessNames() {
    return allowedProcessNames;
  }

  public void setAllowedProcessNames(Set<String> allowedProcessNames) {
    this.allowedProcessNames = allowedProcessNames;
  }
}
