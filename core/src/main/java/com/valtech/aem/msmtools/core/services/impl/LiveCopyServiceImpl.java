package com.valtech.aem.msmtools.core.services.impl;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.day.cq.wcm.msm.api.MSMNameConstants;
import com.day.cq.wcm.msm.api.RolloutManager;
import com.valtech.aem.msmtools.core.helpers.LiveCopyFilter;
import com.valtech.aem.msmtools.core.helpers.LiveCopyStatus;
import com.valtech.aem.msmtools.core.helpers.PublicationStatus;
import com.valtech.aem.msmtools.core.services.LiveCopyService;
import com.valtech.aem.msmtools.core.services.PageService;
import com.valtech.aem.msmtools.core.services.ConfigService;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.jcr.RangeIterator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;

@Slf4j
@Component(immediate = true, service = LiveCopyService.class)
@ServiceDescription("Valtech - MSM Tools - Live Copy Service")
public class LiveCopyServiceImpl implements LiveCopyService {

  public static final String PROP_PREVIOUS_ROLLOUT = "msm-tools-previous-rollout";

  @Reference
  private LiveRelationshipManager liveRelationshipManager;

  @Reference
  private RolloutManager rolloutManager;

  @Reference
  private ConfigService configService;

  @Reference
  private PageService pageService;

  @Override
  public LiveCopyFilter getListFilter(boolean showAllPages) {
    return (status) -> showAllPages
        || status.isCanBeRolledOut()
        || status == LiveCopyStatus.UP_TO_DATE_NOT_REVIEWED;
  }

  public List<LiveRelationship> getRelationships(Resource resource, LiveCopyFilter filter) {
    if (resource == null || ResourceUtil.isNonExistingResource(resource)) {
      return Collections.EMPTY_LIST;
    }
    try {
      if (liveRelationshipManager.hasLiveRelationship(resource)) {
        ResourceResolver resourceResolver = resource.getResourceResolver();
        LiveRelationship liveRelationship = liveRelationshipManager.getLiveRelationship(resource, false);
        return getLiveCopyRelationships(liveRelationship, resourceResolver, filter);
      }
    } catch (WCMException e) {
      log.error("Could not fetch live relationships for live copy " + resource.getPath(), e);
      throw new RuntimeException("Could not fetch live relationships for live copy " + resource.getPath(), e);
    }
    return Collections.EMPTY_LIST;
  }

  protected List<LiveRelationship> getLiveCopyRelationships(LiveRelationship liveRelationship,
      ResourceResolver resourceResolver, LiveCopyFilter filter) throws WCMException {
    List<LiveRelationship> entries = new ArrayList<>();
    if (liveRelationship == null || !liveRelationship.getStatus().isPage()) {
      return entries;
    }
    if (filter.isValid(getStatus(liveRelationship, resourceResolver))) {
      entries.add(liveRelationship);
    }
    RangeIterator children = liveRelationshipManager.getChildren(liveRelationship, resourceResolver);
    while (children.hasNext()) {
      entries.addAll(getLiveCopyRelationships((LiveRelationship) children.next(), resourceResolver, filter));
    }
    return entries;
  }

  @Override
  public LiveCopyStatus getStatus(LiveRelationship liveRelationship, ResourceResolver resourceResolver) {
    List<String> targetMixins = pageService.getMixins(liveRelationship.getTargetPath(), resourceResolver);
    boolean hasTargetLiveRelationshipMixin = targetMixins.contains(MSMNameConstants.NT_LIVE_RELATIONSHIP);
    if (!liveRelationship.getStatus().isSourceExisting()) {
      if (liveRelationship.getStatus().isTargetExisting()) {
        if (!hasTargetLiveRelationshipMixin) {
          return LiveCopyStatus.LOCAL_ONLY;
        }
        return LiveCopyStatus.DELETED;
      }
      return LiveCopyStatus.UNKNOWN;
    }
    Resource blueprintResource = resourceResolver.getResource(liveRelationship.getSourcePath());
    if (configService.blueprintPagesMustBePublished()
        && !PublicationStatus.PUBLISHED.equals(pageService.getPublicationStatus(blueprintResource))) {
      return LiveCopyStatus.NOT_READY;
    }
    if (!liveRelationship.getStatus().isTargetExisting()) {
      return LiveCopyStatus.NEW_PAGE;
    }
    if (!hasTargetLiveRelationshipMixin) {
      return LiveCopyStatus.CONFLICTING;
    }
    if (liveRelationship.getStatus().isCancelled()) {
      return LiveCopyStatus.SUSPENDED;
    }
    boolean isTargetMoved = Optional.ofNullable(getPageMoveTarget(liveRelationship.getTargetPath(), resourceResolver))
        .filter(StringUtils::isNotBlank).isPresent();
    Date rollOutDate = liveRelationship.getStatus().getLastRolledOut();
    Date sourceLastModifiedDate = Optional.ofNullable(blueprintResource)
        .map(res -> res.getChild(JcrConstants.JCR_CONTENT))
        .map(resource -> resource.getValueMap().get(NameConstants.PN_PAGE_LAST_MOD, Date.class))
        .orElse(null);
    if (rollOutDate != null && sourceLastModifiedDate != null && rollOutDate.before(sourceLastModifiedDate)) {
      if (isTargetMoved && configService.pageMoveRolloutActionUsed()) {
        return LiveCopyStatus.MOVED_OUTDATED;
      }
      return LiveCopyStatus.OUTDATED;
    }
    if (isTargetMoved && configService.pageMoveRolloutActionUsed()) {
      return LiveCopyStatus.MOVED;
    }
    if (configService.allowMarkAsDoneAction()) {
      Resource targetPagePropertiesResource = resourceResolver
          .getResource(liveRelationship.getTargetPath() + "/" + JcrConstants.JCR_CONTENT);
      if (targetPagePropertiesResource != null
          && !ResourceUtil.isNonExistingResource(targetPagePropertiesResource)) {
        Date previousRolloutDate = targetPagePropertiesResource.getValueMap()
            .get(PROP_PREVIOUS_ROLLOUT, Date.class);
        if (rollOutDate != null && previousRolloutDate != null
            && previousRolloutDate.getTime() != rollOutDate.getTime()) {
          return LiveCopyStatus.UP_TO_DATE_NOT_REVIEWED;
        }
      }
    }
    return LiveCopyStatus.UP_TO_DATE;
  }

  @Override
  public void rollout(String liveCopyRoot, String[] paths, ResourceResolver resourceResolver)
      throws WCMException, PersistenceException {
    List<LiveRelationship> liveRelationships = getRelationships(
        resourceResolver.getResource(liveCopyRoot), status -> status.isCanBeRolledOut());
    PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
    for (LiveRelationship liveRelationship : liveRelationships) {
      if (!ArrayUtils.contains(paths, liveRelationship.getTargetPath())) {
        continue;
      }
      Page sourcePage = pageManager.getContainingPage(liveRelationship.getSourcePath());
      if (sourcePage != null) {
        Date lastRolloutDate = createVersionAndFetchLastRolloutDate(liveRelationship, resourceResolver);
        // during a move, there is no clean way to get the new target page (though it exists as a property in the
        // liveCopy as moveTarget variable), so we fetch it manually from the nodes/properties:
        String moveTarget = getPageMoveTarget(liveRelationship.getTargetPath(), resourceResolver);
        RolloutManager.RolloutParams params = new RolloutManager.RolloutParams();
        params.master = sourcePage;
        params.isDeep = false;
        params.reset = false;
        params.targets = new String[]{liveRelationship.getTargetPath()};
        rolloutManager.rollout(params);

        Resource resource = resourceResolver
            .getResource(liveRelationship.getTargetPath() + "/" + JcrConstants.JCR_CONTENT);
        if (resource == null && StringUtils.isNotBlank(moveTarget)) {
          // if the target page does not exist anymore because it was moved, the moveTarget needs to be used:
          resource = resourceResolver.getResource(moveTarget + "/" + JcrConstants.JCR_CONTENT);
        }
        if (configService.allowMarkAsDoneAction() && lastRolloutDate != null && resource != null) {
          savePreviousRolloutDate(resource, lastRolloutDate, false);
        } else {
          log.error("Could not set las rollout date because either resource or last rollout date was null");
        }
      } else {
        Resource targetPageResource = resourceResolver.getResource(liveRelationship.getTargetPath());
        if (targetPageResource != null) {
          pageManager.delete(targetPageResource, false);
        }
      }
      if (resourceResolver.hasChanges()) {
        resourceResolver.commit();
      }
    }
  }

  @Override
  public void removeLastRolloutDate(String pagePath, ResourceResolver resourceResolver) throws PersistenceException {
    Resource pagePropertiesResource = resourceResolver.getResource(pagePath + "/" + JcrConstants.JCR_CONTENT);
    if (pagePropertiesResource == null || ResourceUtil.isNonExistingResource(pagePropertiesResource)) {
      return;
    }
    ModifiableValueMap properties = pagePropertiesResource.adaptTo(ModifiableValueMap.class);
    properties.remove(LiveCopyServiceImpl.PROP_PREVIOUS_ROLLOUT);
    resourceResolver.commit();
  }

  protected Date createVersionAndFetchLastRolloutDate(LiveRelationship liveRelationship, ResourceResolver resourceResolver)
      throws WCMException {
    PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
    Page sourcePage = pageManager.getContainingPage(liveRelationship.getSourcePath());
    if (sourcePage == null) {
      return null;
    }
    Date rolloutVersionDate = null;
    if (!hasPageVersionFromLastModification(sourcePage, resourceResolver)) {
      // create a version of the page before performing a rollout:
      Date versionDate = pageService.createVersion(
          sourcePage,
          "MSM Tools Version 1.0",
          "Created when rolling out " + liveRelationship.getTargetPath());
      if (liveRelationship.getStatus().getLastRolledOut() == null) {
        rolloutVersionDate = versionDate;
      }
    }
    if (rolloutVersionDate == null) {
      if (liveRelationship.getStatus().getLastRolledOut() != null) {
        rolloutVersionDate = liveRelationship.getStatus().getLastRolledOut();
      } else {
        // date must be created before the actual rollout happens
        rolloutVersionDate = new Date();
      }
    }
    return rolloutVersionDate;
  }

  private boolean hasPageVersionFromLastModification(Page page, ResourceResolver resourceResolver) {
    if (page.getLastModified() == null) {
      return false;
    }
    String version = pageService.getVersionId(
        page.getPath(),
        page.getLastModified().getTime(),
        new Date(),
        resourceResolver);
    return StringUtils.isNotBlank(version);
  }

  private void savePreviousRolloutDate(Resource resource, Date date, boolean overwrite)
      throws PersistenceException {
    ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    if (!overwrite && properties.get(LiveCopyServiceImpl.PROP_PREVIOUS_ROLLOUT, Date.class) != null) {
      return;
    }
    properties.put(LiveCopyServiceImpl.PROP_PREVIOUS_ROLLOUT, calendar);
    String[] inheritanceCancelled = properties
        .get(MSMNameConstants.PN_PROPERTY_INHERTIANCE_CANCELLED, String[].class);
    if (inheritanceCancelled == null) {
      inheritanceCancelled = new String[]{LiveCopyServiceImpl.PROP_PREVIOUS_ROLLOUT};
    } else if (!ArrayUtils.contains(inheritanceCancelled, LiveCopyServiceImpl.PROP_PREVIOUS_ROLLOUT)) {
      inheritanceCancelled = ArrayUtils.add(inheritanceCancelled, LiveCopyServiceImpl.PROP_PREVIOUS_ROLLOUT);
    } else {
      return;
    }
    properties.put(MSMNameConstants.PN_PROPERTY_INHERTIANCE_CANCELLED, inheritanceCancelled);
    resource.getResourceResolver().commit();
  }

  private String getPageMoveTarget(String pagePath, ResourceResolver resourceResolver) {
    return Optional.ofNullable(resourceResolver
        .getResource(pagePath + "/" + JcrConstants.JCR_CONTENT + "/" + MSMNameConstants.NT_LIVE_SYNC_CONFIG))
        .map(Resource::getValueMap)
        .map(properties -> properties.get(MSMNameConstants.PN_MOVE_TARGET, String.class))
        .orElse(null);
  }
}
