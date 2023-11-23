package com.valtech.aem.msmtools.core.models;

import com.adobe.granite.ui.components.ds.AbstractDataSource;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.valtech.aem.msmtools.core.services.LiveCopyService;
import com.valtech.aem.msmtools.core.services.PageService;
import com.valtech.aem.msmtools.core.services.ConfigService;
import com.valtech.aem.msmtools.core.services.impl.LiveCopyServiceImpl;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

@Slf4j
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class LiveCopyManagerDataSource {

  public static final String PAGE_PATH = "/apps/valtech-msm-tools/pages/livecopymanager.html";

  @SlingObject
  private SlingHttpServletRequest request;

  @OSGiService
  private LiveCopyService liveCopyService;

  @OSGiService
  private ConfigService configService;

  @OSGiService
  private PageService pageService;

  @PostConstruct
  public void setup() {
    Resource liveCopyResource = request.getRequestPathInfo().getSuffixResource();
    request.setAttribute(DataSource.class.getName(), getResourceIterator(liveCopyResource));
  }

  private DataSource getResourceIterator(Resource liveCopyResource) {
    return new AbstractDataSource() {

      @Override
      public Iterator<Resource> iterator() {
        List<Resource> entries = new ArrayList<>();
        if (StringUtils.isBlank(request.getParameter("limit"))) {
          // don't load live copies when generating the page - it will be loaded with lazy loading of the list
          entries.add(new ValueMapResource(
              request.getResourceResolver(), StringUtils.EMPTY, LiveCopyManagerDataItem.RESOURCE_TYPE));
          return entries.iterator();
        }
        List<LiveRelationship> liveRelationships = liveCopyService.getRelationships(
            liveCopyResource,
            liveCopyService.getListFilter(configService.showAllPagesInLiveCopyManager())
        );

        for (int index = 0; index < liveRelationships.size(); index++) {
          entries.add(getLiveCopyEntry(liveRelationships.get(index), request.getResourceResolver()));
        }
        return entries.iterator();
      }

    };
  }

  private Resource getLiveCopyEntry(LiveRelationship liveRelationship, ResourceResolver resourceResolver) {
    ValueMap valueMap = new ValueMapDecorator(new HashMap<>());
    valueMap.put(LiveCopyManagerDataItem.PROP_SOURCE_PATH, liveRelationship.getSourcePath());
    valueMap.put(LiveCopyManagerDataItem.PROP_TARGET_PATH, liveRelationship.getTargetPath());
    valueMap.put(LiveCopyManagerDataItem.PROP_LIVE_COPY_STATUS,
        liveCopyService.getStatus(liveRelationship, resourceResolver).getStatus());
    valueMap.put(LiveCopyManagerDataItem.PROP_LAST_ROLLED_OUT, liveRelationship.getStatus().getLastRolledOut());
    String version = pageService.getVersionId(
        liveRelationship.getSourcePath(),
        null,
        getDateForPageVersionComparison(liveRelationship, resourceResolver),
        resourceResolver);
    valueMap.put(LiveCopyManagerDataItem.PROP_LAST_ROLLED_OUT_VERSION, version);
    return new ValueMapResource(
        request.getResourceResolver(),
        liveRelationship.getTargetPath(),
        LiveCopyManagerDataItem.RESOURCE_TYPE,
        valueMap);
  }

  private Date getDateForPageVersionComparison(LiveRelationship liveRelationship, ResourceResolver resourceResolver) {
    Resource targetPagePropertiesResource = resourceResolver
        .getResource(liveRelationship.getTargetPath() + "/" + JcrConstants.JCR_CONTENT);
    if (configService.allowMarkAsDoneAction()
        && targetPagePropertiesResource != null
        && !ResourceUtil.isNonExistingResource(targetPagePropertiesResource)) {
      Date previousRolloutDate = targetPagePropertiesResource.getValueMap()
          .get(LiveCopyServiceImpl.PROP_PREVIOUS_ROLLOUT, Date.class);
      if (previousRolloutDate != null) {
        return previousRolloutDate;
      }
    }
    return liveRelationship.getStatus().getLastRolledOut();
  }
}
