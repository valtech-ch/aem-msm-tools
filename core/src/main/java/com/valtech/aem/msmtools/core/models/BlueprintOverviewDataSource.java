package com.valtech.aem.msmtools.core.models;

import com.adobe.granite.ui.components.ds.AbstractDataSource;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.jcr.JcrConstants;
import com.valtech.aem.msmtools.core.services.BlueprintService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class BlueprintOverviewDataSource {

  public static final String BLUEPRINT_SITE_CONFIGS_ROOT = "/apps/msm";

  @SlingObject
  private SlingHttpServletRequest request;

  @OSGiService
  private BlueprintService blueprintService;

  @PostConstruct
  public void setup() {
    List<String> paths = getLookupPaths(
        request.getRequestPathInfo().getSuffix(),
        request.getResourceResolver());
    request.setAttribute(
        DataSource.class.getName(),
        getResourceIterator(paths, request.getResourceResolver())
    );
  }

  private DataSource getResourceIterator(final List<String> paths, ResourceResolver resourceResolver) {
    return new AbstractDataSource() {

      @Override
      public Iterator<Resource> iterator() {
        return paths.stream()
            .map(path -> blueprintService.getBlueprints(resourceResolver.getResource(path)))
            .flatMap(List::stream)
            .map(resource -> getEntry(resource))
            .sorted(Comparator.comparing(Resource::getPath))
            .collect(Collectors.toList()).listIterator();
      }
    };
  }

  protected List<String> getLookupPaths(String path, ResourceResolver resourceResolver) {
    List<String> paths = new ArrayList<>();
    if (StringUtils.startsWith(path, BLUEPRINT_SITE_CONFIGS_ROOT)) {
      String sitePath = getSitePathFromBlueprintSiteConfig(path, resourceResolver);
      if (StringUtils.isNotBlank(sitePath)) {
        paths.add(sitePath);
      }
    } else {
      Resource configsRoot = resourceResolver.getResource(BLUEPRINT_SITE_CONFIGS_ROOT);
      if (configsRoot != null) {
        configsRoot.listChildren().forEachRemaining(siteConfig -> {
          String sitePath = getSitePathFromBlueprintSiteConfig(siteConfig.getPath(), resourceResolver);
          if (StringUtils.isNotBlank(sitePath)) {
            paths.add(sitePath);
          }
        });
      }
    }
    return paths;
  }

  private String getSitePathFromBlueprintSiteConfig(String path, ResourceResolver resourceResolver) {
    Resource blueprintConfigResource = resourceResolver.getResource(path + "/" + JcrConstants.JCR_CONTENT);
    if (blueprintConfigResource != null) {
      return blueprintConfigResource.getValueMap().get("sitePath", StringUtils.EMPTY);
    }
    return null;
  }

  private Resource getEntry(Resource blueprintResource) {
    return new ValueMapResource(
        request.getResourceResolver(),
        blueprintResource.getPath(),
        BlueprintOverviewDataItem.RESOURCE_TYPE);
  }
}
