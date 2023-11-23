package com.valtech.aem.msmtools.core.models;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.valtech.aem.msmtools.core.helpers.LiveCopyStatus;
import com.valtech.aem.msmtools.core.helpers.PublicationStatus;
import com.valtech.aem.msmtools.core.services.PageService;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Slf4j
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class LiveCopyManagerDataItem {

  public static final String RESOURCE_TYPE = "valtech-msm-tools/components/livecopymanager/dataitem";

  public static final String PROP_SOURCE_PATH = "sourcePath";
  public static final String PROP_TARGET_PATH = "targetPath";
  public static final String PROP_LIVE_COPY_STATUS = "liveCopyStatus";
  public static final String PROP_LAST_ROLLED_OUT = "lastRolledOut";
  public static final String PROP_LAST_ROLLED_OUT_VERSION = "lastRolledOutVersion";

  @Getter
  @ValueMapValue(name = PROP_LIVE_COPY_STATUS)
  private String liveCopyStatus;

  @Getter
  @ValueMapValue(name = PROP_SOURCE_PATH)
  private String sourcePath;

  @Getter
  @ValueMapValue(name = PROP_TARGET_PATH)
  private String targetPath;

  @Getter
  private String lastRolledOut;

  @Getter
  @ValueMapValue(name = PROP_LAST_ROLLED_OUT_VERSION)
  private String lastRolledOutVersion;

  @Getter
  private String liveCopyStatusName;

  @Getter
  private String pageTitle;

  @Getter
  private DateColumnEntry publicationStatus;

  @SlingObject
  private SlingHttpServletRequest request;

  @OSGiService
  private LiveRelationshipManager liveRelationshipManager;

  @OSGiService
  private PageService pageService;

  @Inject
  private ResourceResolver resourceResolver;

  @ValueMapValue(name = PROP_LAST_ROLLED_OUT)
  private Date lastRolledOutDate;

  private Resource sourceResource;

  private Resource targetResource;

  @PostConstruct
  public void init() {
    sourceResource = resourceResolver.getResource(sourcePath);
    targetResource = resourceResolver.getResource(targetPath);
    lastRolledOut = formatDate(lastRolledOutDate);
    publicationStatus = fetchPublicationStatus(targetResource);
    pageTitle = fetchPageTitle();
  }

  private String fetchPageTitle() {
    String title = getPageProperty(targetResource, JcrConstants.JCR_TITLE, String.class);
    if (StringUtils.isBlank(title)) {
      title = getPageProperty(sourceResource, JcrConstants.JCR_TITLE, String.class);
    }
    return StringUtils.defaultIfBlank(title, StringUtils.EMPTY);
  }

  public String getStatusName() {
    LiveCopyStatus status = LiveCopyStatus.forStatus(liveCopyStatus);
    if (status == null) {
      return StringUtils.EMPTY;
    }
    return status.getDisplayName();
  }

  protected DateColumnEntry fetchPublicationStatus(Resource pageResource) {
    PublicationStatus status = pageService.getPublicationStatus(pageResource);
    switch (status) {
      case PUBLISHED:
        return new DateColumnEntry(
            getPageProperty(pageResource, NameConstants.PN_PAGE_LAST_REPLICATED, Date.class),
            status.getDisplayName());
      case MODIFIED:
        return new DateColumnEntry(
            getPageProperty(pageResource, NameConstants.PN_PAGE_LAST_MOD, Date.class),
            status.getDisplayName());
      case NOT_PUBLISHED:
        return new DateColumnEntry(null, status.getDisplayName());
      default:
        return new DateColumnEntry(null, null);
    }
  }

  public String getLiveCopyRootPath() {
    return request.getRequestPathInfo().getSuffix();
  }

  private <T> T getPageProperty(Resource pageResource, String name, Class<T> type) {
    if (pageResource == null) {
      return null;
    }
    Resource propertiesResource = pageResource.getChild(JcrConstants.JCR_CONTENT);
    if (propertiesResource == null) {
      return null;
    }
    return propertiesResource.getValueMap().get(name, type);
  }

  private String formatDate(final Date date) {
    if (date == null) {
      return StringUtils.EMPTY;
    }
    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
  }

  @AllArgsConstructor
  public class DateColumnEntry {

    private Date date;

    @Getter
    private String text;

    public String getDate() {
      return formatDate(date);
    }
  }
}
