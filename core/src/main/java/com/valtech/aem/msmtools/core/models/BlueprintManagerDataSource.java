package com.valtech.aem.msmtools.core.models;

import com.adobe.granite.ui.components.ds.AbstractDataSource;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.valtech.aem.msmtools.core.services.BlueprintService;
import com.valtech.aem.msmtools.core.services.ConfigService;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
public class BlueprintManagerDataSource {

  public static final String PAGE_PATH = "/apps/valtech-msm-tools/pages/blueprintmanager.html";

  @SlingObject
  private SlingHttpServletRequest request;

  @OSGiService
  private BlueprintService blueprintService;

  @OSGiService
  private ConfigService configService;

  @PostConstruct
  public void setup() {
    String path = request.getRequestPathInfo().getSuffix();
    request.setAttribute(
        DataSource.class.getName(),
        getResourceIterator(path, request.getResourceResolver())
    );
  }

  private DataSource getResourceIterator(final String path, ResourceResolver resourceResolver) {
    return new AbstractDataSource() {

      @Override
      public Iterator<Resource> iterator() {
        if (StringUtils.isBlank(path)) {
          return Collections.EMPTY_LIST.iterator();
        }
        return Stream.of(path)
            .map(path -> blueprintService.getRelationships(resourceResolver.getResource(path)))
            .flatMap(List::stream)
            .map(liveRelationship -> getEntry(liveRelationship))
            .sorted(Comparator.comparing(Resource::getPath))
            .collect(Collectors.toList()).listIterator();
      }
    };
  }

  private Resource getEntry(LiveRelationship liveRelationship) {
    return new ValueMapResource(
        request.getResourceResolver(),
        liveRelationship.getTargetPath(),
        BlueprintManagerDataItem.RESOURCE_TYPE);
  }
}
