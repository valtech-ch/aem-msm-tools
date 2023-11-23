package com.valtech.aem.msmtools.core.models;

import com.valtech.aem.msmtools.core.services.PageService;
import com.valtech.aem.msmtools.core.services.ConfigService;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

@Slf4j
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class BlueprintOverviewDataItem {

  public static final String RESOURCE_TYPE = "valtech-msm-tools/components/blueprintoverview/dataitem";

  @Getter
  private String region;

  @Getter
  private String language;

  @Getter
  private String blueprintPath;

  @SlingObject
  private SlingHttpServletRequest request;

  @OSGiService
  private PageService pageService;

  @OSGiService
  private ConfigService msmToolConfigService;

  @Inject
  private ResourceResolver resourceResolver;

  @PostConstruct
  public void init() {
    blueprintPath = request.getResource().getPath();
    region = StringUtils.upperCase(pageService.getRegion(blueprintPath));
    language = StringUtils.upperCase(pageService.getLanguage(blueprintPath));
  }

  public String getBlueprintManagerPage() {
    return BlueprintManagerDataSource.PAGE_PATH;
  }
}
