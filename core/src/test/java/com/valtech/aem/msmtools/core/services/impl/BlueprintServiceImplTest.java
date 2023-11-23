package com.valtech.aem.msmtools.core.services.impl;

import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.valtech.aem.msmtools.core.MsmHelper;
import com.valtech.aem.msmtools.core.services.BlueprintService;
import com.valtech.aem.msmtools.core.services.ConfigService;
import com.valtech.aem.msmtools.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.util.Arrays;
import java.util.List;
import javax.jcr.RangeIterator;
import junit.framework.Assert;
import org.apache.jackrabbit.commons.iterator.RangeIteratorAdapter;
import org.apache.sling.api.resource.Resource;
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
class BlueprintServiceImplTest {

  private AemContext context = AppAemContext.newAemContextBuilder().build();

  private BlueprintService blueprintService;

  @Mock private LiveRelationshipManager liveRelationshipManager;
  @Mock private ConfigService configService;

  @BeforeEach
  public void setUp() {
    context.registerService(LiveRelationshipManager.class, liveRelationshipManager);
    context.registerService(ConfigService.class, configService);
    context.load().json("/com/valtech/aem/msmtools/core/services/msm-configs.json", "/apps");
    context.load().json("/com/valtech/aem/msmtools/core/services/msm-sites.json", "/content");
    blueprintService = context.registerInjectActivateService(new BlueprintServiceImpl());
  }

  @Test
  void getBlueprint() throws WCMException {
    Assert.assertNull("Non existing resource", blueprintService.getBlueprint(null));

    Resource notLiveCopy = context.resourceResolver().getResource("/content/msm-sites/ch/de");
    Assert.assertNull("No live relationship for given resource", blueprintService.getBlueprint(notLiveCopy));

    Resource existingLiveCopy = context.resourceResolver().getResource("/content/msm-sites/ch/en");
    LiveRelationship liveRelationship = MsmHelper.createLiveRelationship("/content/msm-sites/language-masters/en");
    Mockito.when(liveRelationshipManager.getLiveRelationship(existingLiveCopy, false)).thenReturn(liveRelationship);
    Assert.assertNotNull("Has blueprint resource", blueprintService.getBlueprint(existingLiveCopy));
  }

  @Test
  void getBlueprints() throws WCMException {
    Assert.assertEquals("Non existing resource", 0, blueprintService.getBlueprints(null).size());

    Resource notBlueprint = context.resourceResolver().getResource("/content/msm-sites/ch/de");
    Mockito.when(liveRelationshipManager.getLiveRelationships(notBlueprint, null, null)).thenReturn(RangeIteratorAdapter.EMPTY);
    Assert.assertEquals("Non existing resource", 0, blueprintService.getBlueprints(notBlueprint).size());

    RangeIterator withSingleLiveCopyIterator = new RangeIteratorAdapter(Arrays.asList(
        MsmHelper.createLiveRelationship("/content/msm-sites/language-masters/fr")
    ));
    RangeIterator withMultipleLiveCopiesIterator = new RangeIteratorAdapter(Arrays.asList(
        MsmHelper.createLiveRelationship("/content/msm-sites/language-masters/en"),
        MsmHelper.createLiveRelationship("/content/msm-sites/language-masters/en")
    ));
    RangeIterator nonRootLiveCopyIterator = new RangeIteratorAdapter(Arrays.asList(
        MsmHelper.createLiveRelationship("/content/msm-sites/language-masters/en/page1", false)
    ));
    Mockito.when(liveRelationshipManager.getLiveRelationships(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenAnswer(invocationOnMock -> {
          Resource resource = invocationOnMock.getArgument(0);
          if ("/content/msm-sites/language-masters/fr".equals(resource.getPath())) {
            return withSingleLiveCopyIterator;
          }
          if ("/content/msm-sites/language-masters/en".equals(resource.getPath())) {
            return withMultipleLiveCopiesIterator;
          }
          if ("/content/msm-sites/language-masters/en/page1".equals(resource.getPath())) {
            return nonRootLiveCopyIterator;
          }
          return RangeIteratorAdapter.EMPTY;
        });
    Resource siteRoot = context.resourceResolver().getResource("/content/msm-sites");
    Mockito.when(configService.getBlueprintRootMaxDepth()).thenReturn(2);
    Assert.assertEquals("Blueprint with one live copy, not enough depth", 0, blueprintService.getBlueprints(siteRoot).size());

    Mockito.when(configService.getBlueprintRootMaxDepth()).thenReturn(3);
    Assert.assertEquals("Blueprint with one live copy", 2, blueprintService.getBlueprints(siteRoot).size());
    Assert.assertEquals("Resource is not root", 0, blueprintService.getBlueprints(context.resourceResolver().getResource("/content/msm-sites/language-masters/en/page1")).size());
  }

  @Test
  void getRelationships() throws WCMException {
    Assert.assertEquals("Non existing resource", 0, blueprintService.getRelationships(null).size());

    Resource notBlueprint = context.resourceResolver().getResource("/content/msm-sites/ch/de");
    Mockito.when(liveRelationshipManager.getLiveRelationships(notBlueprint, null, null)).thenReturn(RangeIteratorAdapter.EMPTY);
    Assert.assertEquals("Non existing resource", 0, blueprintService.getRelationships(notBlueprint).size());

    RangeIterator withSingleLiveCopyIterator = new RangeIteratorAdapter(Arrays.asList(
        MsmHelper.createLiveRelationship("/content/msm-sites/language-masters/fr", "/content/msm-sites/fr/fr", true)
    ));
    RangeIterator withMultipleLiveCopiesIterator = new RangeIteratorAdapter(Arrays.asList(
        MsmHelper.createLiveRelationship("/content/msm-sites/language-masters/en", "/content/msm-sites/fr/en", true),
        MsmHelper.createLiveRelationship("/content/msm-sites/language-masters/en", "/content/msm-sites/ch/en", true)
    ));
    RangeIterator nestedLiveRelationshipIterator = new RangeIteratorAdapter(Arrays.asList(
        MsmHelper.createLiveRelationship("/content/msm-sites/fr/fr", "/content/msm-sites/ch/fr", true)
    ));
    Mockito.when(liveRelationshipManager.getLiveRelationships(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenAnswer(invocationOnMock -> {
          Resource resource = invocationOnMock.getArgument(0);
          if ("/content/msm-sites/language-masters/fr".equals(resource.getPath())) {
            return withSingleLiveCopyIterator;
          }
          if ("/content/msm-sites/language-masters/en".equals(resource.getPath())) {
            return withMultipleLiveCopiesIterator;
          }
          if ("/content/msm-sites/fr/fr".equals(resource.getPath())) {
            return nestedLiveRelationshipIterator;
          }
          return RangeIteratorAdapter.EMPTY;
        });
    List<LiveRelationship> relationships = blueprintService.getRelationships(context.resourceResolver().getResource("/content/msm-sites/language-masters/en"));
    Assert.assertEquals("Blueprint with single level live copies", 2, relationships.size());
    Assert.assertEquals("Correct live copy path en-FR", "/content/msm-sites/fr/en", relationships.get(0).getTargetPath());
    Assert.assertEquals("Correct live copy path en-CH", "/content/msm-sites/ch/en", relationships.get(1).getTargetPath());

    Mockito.when(configService.resolveNestedLiveCopies()).thenReturn(true);
    relationships = blueprintService.getRelationships(context.resourceResolver().getResource("/content/msm-sites/language-masters/fr"));
    Assert.assertEquals("Blueprint with nested live copies", 2, relationships.size());
    Assert.assertEquals("Correct live copy path fr-FR", "/content/msm-sites/fr/fr", relationships.get(0).getTargetPath());
    Assert.assertEquals("Correct live copy path fr-CH", "/content/msm-sites/ch/fr", relationships.get(1).getTargetPath());
  }
}