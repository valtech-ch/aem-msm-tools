package com.valtech.aem.msmtools.core.services.impl;

import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.day.cq.wcm.msm.api.MSMNameConstants;
import com.day.cq.wcm.msm.api.RolloutManager;
import com.day.cq.wcm.msm.api.RolloutManager.RolloutParams;
import com.valtech.aem.msmtools.core.MsmHelper;
import com.valtech.aem.msmtools.core.helpers.LiveCopyFilter;
import com.valtech.aem.msmtools.core.helpers.LiveCopyStatus;
import com.valtech.aem.msmtools.core.helpers.PublicationStatus;
import com.valtech.aem.msmtools.core.services.LiveCopyService;
import com.valtech.aem.msmtools.core.services.PageService;
import com.valtech.aem.msmtools.core.services.ConfigService;
import com.valtech.aem.msmtools.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import junit.framework.Assert;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.commons.iterator.RangeIteratorAdapter;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith({MockitoExtension.class, AemContextExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class LiveCopyServiceImplTest {

  private AemContext context = AppAemContext.newAemContextBuilder().build();

  private LiveCopyServiceImpl liveCopyService;

  @Mock private LiveRelationshipManager liveRelationshipManager;

  @Mock private ConfigService configService;

  @Mock private RolloutManager rolloutManager;

  @Mock private PageService pageService;

  @BeforeEach
  void setUp() {
    context.registerService(LiveRelationshipManager.class, liveRelationshipManager);
    context.registerService(ConfigService.class, configService);
    context.registerService(RolloutManager.class, rolloutManager);
    context.registerService(PageService.class, pageService);
    liveCopyService = context.registerInjectActivateService(new LiveCopyServiceImpl());

    Mockito.doAnswer(invocationOnMock -> {
      String path = invocationOnMock.getArgument(0, String.class);
      return MsmHelper.getMixins(context.resourceResolver(), path);
    }).when(pageService).getMixins(Mockito.anyString(), Mockito.any(ResourceResolver.class));
    Mockito.doAnswer(invocationOnMock -> {
      Resource resource = invocationOnMock.getArgument(0, Resource.class);
      return resource.getValueMap().get("jcr:content/cq:lastReplicated") == null ? PublicationStatus.NOT_PUBLISHED : PublicationStatus.PUBLISHED;
    }).when(pageService).getPublicationStatus(Mockito.any(Resource.class));
  }

  @Test
  void getListFilter() {
    LiveCopyFilter filter = liveCopyService.getListFilter(false);
    Assert.assertFalse("Excludes up-to-date pages", filter.isValid(LiveCopyStatus.UP_TO_DATE));
    Assert.assertTrue("Includes outdated pages", filter.isValid(LiveCopyStatus.OUTDATED));
    filter = liveCopyService.getListFilter(true);
    Assert.assertTrue("Includes up-to-date pages", filter.isValid(LiveCopyStatus.UP_TO_DATE));
    Assert.assertTrue("Includes outdated pages", filter.isValid(LiveCopyStatus.OUTDATED));
  }

  @Test
  void getRelationships() throws WCMException {
    context.load().json("/com/valtech/aem/msmtools/core/services/msm-sites.json", "/content");
    prepareLiveCopy("/content/msm-sites/language-masters/en", "/content/msm-sites/ch/en");
    LiveCopyService spyService = Mockito.spy(liveCopyService);
    Mockito.doAnswer(invocationOnMock -> {
      if (invocationOnMock.getArgument(0, LiveRelationship.class).getTargetPath().equals("/content/msm-sites/ch/en/page-1")) {
        return LiveCopyStatus.UP_TO_DATE;
      }
      return LiveCopyStatus.OUTDATED;
    }).when(spyService).getStatus(Mockito.any(LiveRelationship.class), Mockito.any(ResourceResolver.class));
    LiveCopyFilter filter = liveCopyService.getListFilter(false);

    Assert.assertEquals("Empty if invalid resource", 0, liveCopyService.getRelationships(null, filter).size());
    Assert.assertEquals("Empty if not a live copy", 0, liveCopyService.getRelationships(
        context.resourceResolver().getResource("/content/msm-sites/ch/de"), filter).size());

    List<LiveRelationship> relationships = spyService.getRelationships(context.resourceResolver().getResource("/content/msm-sites/ch/en"), filter);
    Assert.assertEquals("Single live copy", 2, relationships.size());
    Assert.assertTrue("Correct live relationship 1", relationships.stream().anyMatch(lr -> lr.getTargetPath().equals("/content/msm-sites/ch/en")));
    Assert.assertTrue("Correct live relationship 2", relationships.stream().anyMatch(lr -> lr.getTargetPath().equals("/content/msm-sites/ch/en/page-1/page-1-1")));
    Assert.assertTrue("Correct usage of filter", relationships.stream().noneMatch(lr -> lr.getTargetPath().equals("/content/msm-sites/ch/en/page-1")));
  }

  @Test
  void getStatus() {
    context.load().json("/com/valtech/aem/msmtools/core/services/live-copy-status.json", "/content");
    ResourceResolver rr = context.resourceResolver();

    LiveRelationship lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/new", "/content/live-copy-status/live-copy/new");
    Assert.assertEquals(LiveCopyStatus.NEW_PAGE.name() + " status check", LiveCopyStatus.NEW_PAGE, liveCopyService.getStatus(lr, context.resourceResolver()));
    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/deleted", "/content/live-copy-status/live-copy/deleted");
    Assert.assertEquals(LiveCopyStatus.DELETED.name() + " status check", LiveCopyStatus.DELETED, liveCopyService.getStatus(lr, context.resourceResolver()));
    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/local-only", "/content/live-copy-status/live-copy/local-only");
    Assert.assertEquals(LiveCopyStatus.LOCAL_ONLY.name() + " status check", LiveCopyStatus.LOCAL_ONLY, liveCopyService.getStatus(lr, context.resourceResolver()));
    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/unknown", "/content/live-copy-status/live-copy/unknown");
    Assert.assertEquals(LiveCopyStatus.UNKNOWN.name() + " status check", LiveCopyStatus.UNKNOWN, liveCopyService.getStatus(lr, context.resourceResolver()));
    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/conflicting", "/content/live-copy-status/live-copy/conflicting");
    Assert.assertEquals(LiveCopyStatus.CONFLICTING.name() + " status check", LiveCopyStatus.CONFLICTING, liveCopyService.getStatus(lr, context.resourceResolver()));
    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/suspended", "/content/live-copy-status/live-copy/suspended");
    Assert.assertEquals(LiveCopyStatus.SUSPENDED.name() + " status check", LiveCopyStatus.SUSPENDED, liveCopyService.getStatus(lr, context.resourceResolver()));
    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/up-to-date-no-dates", "/content/live-copy-status/live-copy/up-to-date-no-dates");
    Assert.assertEquals(LiveCopyStatus.UP_TO_DATE.name() + " status check, no date indicators", LiveCopyStatus.UP_TO_DATE, liveCopyService.getStatus(lr, context.resourceResolver()));
    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/up-to-date-with-modified-date", "/content/live-copy-status/live-copy/up-to-date-with-modified-date");
    Assert.assertEquals(LiveCopyStatus.UP_TO_DATE.name() + " status check, with modified date only", LiveCopyStatus.UP_TO_DATE, liveCopyService.getStatus(lr, context.resourceResolver()));
    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/up-to-date-with-rollout-date", "/content/live-copy-status/live-copy/up-to-date-with-rollout-date");
    Assert.assertEquals(LiveCopyStatus.UP_TO_DATE.name() + " status check, with rollout date only", LiveCopyStatus.UP_TO_DATE, liveCopyService.getStatus(lr, context.resourceResolver()));
    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/up-to-date-with-all-dates", "/content/live-copy-status/live-copy/up-to-date-with-all-dates");
    Assert.assertEquals(LiveCopyStatus.UP_TO_DATE.name() + " status check, with both dates", LiveCopyStatus.UP_TO_DATE, liveCopyService.getStatus(lr, context.resourceResolver()));
    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/outdated", "/content/live-copy-status/live-copy/outdated");
    Assert.assertEquals(LiveCopyStatus.OUTDATED.name() + " status check", LiveCopyStatus.OUTDATED, liveCopyService.getStatus(lr, context.resourceResolver()));
    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/moved", "/content/live-copy-status/live-copy/moved");
    Assert.assertEquals(LiveCopyStatus.MOVED.name() + " status check when action not used", LiveCopyStatus.UP_TO_DATE, liveCopyService.getStatus(lr, context.resourceResolver()));
    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/moved-outdated", "/content/live-copy-status/live-copy/moved-outdated");
    Assert.assertEquals(LiveCopyStatus.MOVED_OUTDATED.name() + " status check when action not used", LiveCopyStatus.OUTDATED, liveCopyService.getStatus(lr, context.resourceResolver()));

    Mockito.doReturn(true).when(configService).pageMoveRolloutActionUsed();
    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/moved", "/content/live-copy-status/live-copy/moved");
    Assert.assertEquals(LiveCopyStatus.MOVED.name() + " status check", LiveCopyStatus.MOVED, liveCopyService.getStatus(lr, context.resourceResolver()));
    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/moved-outdated", "/content/live-copy-status/live-copy/moved-outdated");
    Assert.assertEquals(LiveCopyStatus.MOVED_OUTDATED.name() + " status check", LiveCopyStatus.MOVED_OUTDATED, liveCopyService.getStatus(lr, context.resourceResolver()));

    Mockito.doReturn(true).when(configService).allowMarkAsDoneAction();
    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/needs-review", "/content/live-copy-status/live-copy/needs-review");
    Assert.assertEquals(LiveCopyStatus.UP_TO_DATE_NOT_REVIEWED.name() + " status check", LiveCopyStatus.UP_TO_DATE_NOT_REVIEWED, liveCopyService.getStatus(lr, context.resourceResolver()));

    Mockito.doReturn(true).when(configService).blueprintPagesMustBePublished();
    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/new", "/content/live-copy-status/live-copy/new");
    Assert.assertEquals(LiveCopyStatus.NEW_PAGE.name() + " status check for non-published blueprint", LiveCopyStatus.NOT_READY, liveCopyService.getStatus(lr, context.resourceResolver()));
    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/up-to-date-with-all-dates", "/content/live-copy-status/live-copy/up-to-date-with-all-dates");
    Assert.assertEquals(LiveCopyStatus.UP_TO_DATE.name() + " status check for non-published blueprint", LiveCopyStatus.NOT_READY, liveCopyService.getStatus(lr, context.resourceResolver()));
    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/outdated", "/content/live-copy-status/live-copy/outdated");
    Assert.assertEquals(LiveCopyStatus.OUTDATED.name() + " status check for non-published blueprint", LiveCopyStatus.NOT_READY, liveCopyService.getStatus(lr, context.resourceResolver()));

    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/new-published", "/content/live-copy-status/live-copy/new-published");
    Assert.assertEquals(LiveCopyStatus.NEW_PAGE.name() + " status check for published blueprint", LiveCopyStatus.NEW_PAGE, liveCopyService.getStatus(lr, context.resourceResolver()));
    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/up-to-date-with-all-dates-published", "/content/live-copy-status/live-copy/up-to-date-with-all-dates-published");
    Assert.assertEquals(LiveCopyStatus.UP_TO_DATE.name() + " status check for published blueprint", LiveCopyStatus.UP_TO_DATE, liveCopyService.getStatus(lr, context.resourceResolver()));
    lr = MsmHelper.createLiveRelationship(rr, "/content/live-copy-status/blueprint/outdated-published", "/content/live-copy-status/live-copy/outdated-published");
    Assert.assertEquals(LiveCopyStatus.OUTDATED.name() + " status check for published blueprint", LiveCopyStatus.OUTDATED, liveCopyService.getStatus(lr, context.resourceResolver()));
  }

  @Test
  void rollout() throws WCMException, PersistenceException {
    List<String> rolledOutPages = new ArrayList<>();
    Mockito.doAnswer(invocationOnMock -> {
      RolloutParams params = invocationOnMock.getArgument(0, RolloutParams.class);
      rolledOutPages.addAll(Arrays.asList(params.targets));
      return null;
    }).when(rolloutManager).rollout(Mockito.any(RolloutParams.class));
    Mockito.doReturn(true).when(configService).pageMoveRolloutActionUsed();
    Mockito.doReturn(true).when(configService).allowMarkAsDoneAction();

    context.load().json("/com/valtech/aem/msmtools/core/services/live-copy-status.json", "/content");
    prepareLiveCopy("/content/live-copy-status/blueprint", "/content/live-copy-status/live-copy");
    liveCopyService.rollout("/content/live-copy-status/live-copy", new String[] {
        "/content/live-copy-status/live-copy/outdated",
        "/content/live-copy-status/live-copy/outdated2",
        "/content/live-copy-status/live-copy/outdated3",
        "/content/live-copy-status/live-copy/moved"
    }, context.resourceResolver());
    ValueMap properties = context.resourceResolver().getResource("/content/live-copy-status/live-copy/outdated/jcr:content").getValueMap();
    Assert.assertTrue("Outdated page rolled out", rolledOutPages.contains("/content/live-copy-status/live-copy/outdated"));
    Assert.assertNotNull("Outdated has last rollout property", properties.get(LiveCopyServiceImpl.PROP_PREVIOUS_ROLLOUT));
    Assert.assertTrue("Outdated has cancelled inheritance property",
        ArrayUtils.contains(properties.get(MSMNameConstants.PN_PROPERTY_INHERTIANCE_CANCELLED, new String[0]), LiveCopyServiceImpl.PROP_PREVIOUS_ROLLOUT));
    properties = context.resourceResolver().getResource("/content/live-copy-status/live-copy/outdated2/jcr:content").getValueMap();
    Assert.assertTrue("Outdated2 page rolled out", rolledOutPages.contains("/content/live-copy-status/live-copy/outdated2"));
    Assert.assertNotNull("Outdated has last rollout property", properties.get(LiveCopyServiceImpl.PROP_PREVIOUS_ROLLOUT));
    Assert.assertTrue("Outdated2 has cancelled inheritance property",
        ArrayUtils.contains(properties.get(MSMNameConstants.PN_PROPERTY_INHERTIANCE_CANCELLED, new String[0]), LiveCopyServiceImpl.PROP_PREVIOUS_ROLLOUT));
    properties = context.resourceResolver().getResource("/content/live-copy-status/live-copy/outdated3/jcr:content").getValueMap();
    Assert.assertTrue("Outdated3 page rolled out", rolledOutPages.contains("/content/live-copy-status/live-copy/outdated3"));
    Assert.assertNotNull("Outdated3 has last rollout property", properties.get(LiveCopyServiceImpl.PROP_PREVIOUS_ROLLOUT));
    Assert.assertTrue("Outdated3 has cancelled inheritance property",
        ArrayUtils.contains(properties.get(MSMNameConstants.PN_PROPERTY_INHERTIANCE_CANCELLED, new String[0]), LiveCopyServiceImpl.PROP_PREVIOUS_ROLLOUT));

    Assert.assertTrue("Moved page rolled out", rolledOutPages.contains("/content/live-copy-status/live-copy/moved"));

    liveCopyService.rollout("/content/live-copy-status/live-copy", new String[] {"/content/live-copy-status/live-copy/deleted"}, context.resourceResolver());
    Assert.assertNull("Deleted page", context.resourceResolver().getResource("/content/live-copy-status/live-copy/deleted"));
  }

  @Test
  void removeLastRolloutDate() throws PersistenceException {
    context.load().json("/com/valtech/aem/msmtools/core/services/live-copy-status.json", "/content");
    liveCopyService.removeLastRolloutDate("/content/live-copy-status/live-copy/needs-review", context.resourceResolver());
    Assert.assertNull("Last rollout date removed",
        context.resourceResolver().getResource("/content/live-copy-status/live-copy/needs-review").getValueMap().get(LiveCopyServiceImpl.PROP_PREVIOUS_ROLLOUT));
  }

  private void prepareLiveCopy(String blueprintRoot, String liveCopyRoot) throws WCMException {
    Map<String, LiveRelationship> liveRelationshipMap = new HashMap<>();
    Map<String, List<String>> contentTree = new HashMap<>();
    Resource resource = context.resourceResolver().getResource(liveCopyRoot);
    liveRelationshipMap.putAll(fetchLiveRelationships(blueprintRoot, liveCopyRoot, resource));
    contentTree.putAll(fetchContentTree(resource));
    Mockito.when(liveRelationshipManager.hasLiveRelationship(Mockito.any(Resource.class))).thenAnswer(
        invocationOnMock -> liveRelationshipMap.containsKey(((Resource) invocationOnMock.getArgument(0)).getPath())
    );
    Mockito.when(liveRelationshipManager.getLiveRelationship(Mockito.any(Resource.class), Mockito.anyBoolean())).thenAnswer(
        invocationOnMock -> liveRelationshipMap.get(((Resource) invocationOnMock.getArgument(0)).getPath())
    );
    Mockito.when(liveRelationshipManager.getChildren(Mockito.any(LiveRelationship.class), Mockito.any(ResourceResolver.class))).thenAnswer(invocationOnMock -> {
      List<String> resourcePaths = contentTree.get(((LiveRelationship) invocationOnMock.getArgument(0)).getTargetPath());
      if (resourcePaths != null) {
        return new RangeIteratorAdapter(resourcePaths.stream()
            .map(path -> liveRelationshipMap.get(path))
            .filter(Objects::nonNull)
            .collect(Collectors.toList())
        );
      }
      return RangeIteratorAdapter.EMPTY;
    });
  }

  private Map<String, LiveRelationship> fetchLiveRelationships(String blueprintRoot, String liveCopyRoot, Resource resource) {
    Map<String, LiveRelationship> liveRelationshipMap = new HashMap<>();
    LiveRelationship lr = MsmHelper.createLiveRelationship(resource.getResourceResolver(), StringUtils.replace(resource.getPath(), liveCopyRoot, blueprintRoot), resource.getPath());
    liveRelationshipMap.put(resource.getPath(), lr);
    resource.listChildren().forEachRemaining(child -> liveRelationshipMap.putAll(fetchLiveRelationships(blueprintRoot, liveCopyRoot, child)));
    return liveRelationshipMap;
  }

  private Map<String, List<String>> fetchContentTree(Resource resource) {
    Map<String, List<String>> contentTree = new HashMap<>();
    List<String> children = new ArrayList<>();
    contentTree.put(resource.getPath(), children);
    resource.listChildren().forEachRemaining(child -> {
      children.add(child.getPath());
      contentTree.putAll(fetchContentTree(child));
    });
    return contentTree;
  }
}