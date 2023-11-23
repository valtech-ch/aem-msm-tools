package com.valtech.aem.msmtools.core.services.impl;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.replication.ReplicationStatus;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.Revision;
import com.day.cq.wcm.api.WCMException;
import com.valtech.aem.msmtools.core.helpers.PublicationStatus;
import com.valtech.aem.msmtools.core.services.PageService;
import com.valtech.aem.msmtools.core.services.ConfigService;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;

@Slf4j
@Component(immediate = true, service = PageService.class)
@ServiceDescription("Valtech - MSM Tools - Page Service")
public class PageServiceImpl implements PageService {

  @Reference
  private ConfigService configService;

  @Override
  public PublicationStatus getPublicationStatus(Resource pageResource) {
    if (pageResource == null) {
      return PublicationStatus.UNKNOWN;
    }
    Page page = pageResource.adaptTo(Page.class);
    ReplicationStatus replicationStatus = pageResource.adaptTo(ReplicationStatus.class);
    if (page == null || replicationStatus == null) {
      return PublicationStatus.UNKNOWN;
    }
    if (!replicationStatus.isActivated() || replicationStatus.isDeactivated()) {
      return PublicationStatus.NOT_PUBLISHED;
    }
    if (page.getLastModified() == null || replicationStatus.getLastPublished() == null) {
      return PublicationStatus.PUBLISHED;
    }
    if (replicationStatus.getLastPublished().before(page.getLastModified())) {
      return PublicationStatus.MODIFIED;
    }
    return PublicationStatus.PUBLISHED;
  }

  @Override
  public String getRegion(String pagePath) {
    return getPathPartByDepth(pagePath, configService.getRegionPageDepth());
  }

  @Override
  public String getLanguage(String pagePath) {
    return getPathPartByDepth(pagePath, configService.getLanguagePageDepth());
  }

  @Override
  public String getVersionId(String pagePath, Date fromDate, Date toDate, ResourceResolver resourceResolver) {
    if (toDate == null) {
      return StringUtils.EMPTY;
    }
    Resource pageResource = resourceResolver.getResource(pagePath);
    if (pageResource == null) {
      return StringUtils.EMPTY;
    }
    Calendar toCalendar = null, fromCalendar = null;
    if (toDate != null) {
      toCalendar = Calendar.getInstance();
      toCalendar.setTime(toDate);
    }
    if (fromDate != null) {
      fromCalendar = Calendar.getInstance();
      fromCalendar.setTime(fromDate);
    }
    try {
      Session session = resourceResolver.adaptTo(Session.class);
      VersionManager versionManager = session.getWorkspace().getVersionManager();
      VersionHistory versionHistory = versionManager.getVersionHistory(pagePath + "/" + JcrConstants.JCR_CONTENT);
      Version version = null;
      VersionIterator versionIterator = versionHistory.getAllVersions();
      while (versionIterator.hasNext()) {
        Version currentVersion = versionIterator.nextVersion();
        if (fromCalendar != null && toCalendar != null) {
          if ((currentVersion.getCreated().after(fromCalendar)
              || currentVersion.getCreated().compareTo(fromCalendar) == 0)
              && (currentVersion.getCreated().before(toCalendar)
              || currentVersion.getCreated().compareTo(toCalendar) == 0)) {
            version = currentVersion;
          }
          if (currentVersion.getCreated().after(toCalendar)) {
            break;
          }
        } else if (fromCalendar != null) {
          if (currentVersion.getCreated().after(fromCalendar)
              || currentVersion.getCreated().compareTo(fromCalendar) == 0) {
            version = currentVersion;
          }
        } else if (toCalendar != null) {
          if (currentVersion.getCreated().before(toCalendar)
              || currentVersion.getCreated().compareTo(toCalendar) == 0) {
            version = currentVersion;
          } else {
            break;
          }
        }
      }
      if (version != null && !StringUtils.equals(version.getName(), JcrConstants.JCR_ROOTVERSION)) {
        return version.getIdentifier();
      }
    } catch (RepositoryException e) {
      log.warn("Could not fetch version for the page " + pagePath);
    }
    return StringUtils.EMPTY;
  }

  @Override
  public Date createVersion(Page page, String label, String comment) throws WCMException {
    Revision revision = page.getPageManager().createRevision(
        page,
        label,
        comment);
    return Optional.ofNullable(revision)
        .map(r -> r.getCreated())
        .map(c -> c.getTime())
        .orElse(null);
  }

  @Override
  public List<String> getMixins(String pagePath, ResourceResolver resourceResolver) {
    NodeType[] nodeTypes = Optional.ofNullable(pagePath)
        .map(path -> resourceResolver.getResource(path + "/" + JcrConstants.JCR_CONTENT))
        .map(pageContentResource -> pageContentResource.adaptTo(Node.class))
        .map(node -> {
          try {
            NodeType[] mixins = node.getMixinNodeTypes();
            if (mixins != null) {
              return mixins;
            }
          } catch (RepositoryException e) {
            log.error("Could not fetch mixin types for page " + pagePath, e);
          }
          return new NodeType[0];
        })
        .orElse(new NodeType[0]);
    return Arrays.stream(nodeTypes)
        .map(nodeType -> nodeType.getName())
        .collect(Collectors.toList());
  }

  private String getPathPartByDepth(String path, int depth) {
    String[] splitString = StringUtils.split(path, "/");
    if (splitString != null && splitString.length > depth) {
      return splitString[depth];
    }
    return StringUtils.EMPTY;
  }
}
