package com.valtech.aem.msmtools.core.models;

import javax.annotation.PostConstruct;
import lombok.Getter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class BlueprintManagerTitle {

  @Getter
  private String path;

  @SlingObject
  private SlingHttpServletRequest request;

  @PostConstruct
  public void setup() {
    path = request.getRequestPathInfo().getSuffix();
  }
}
