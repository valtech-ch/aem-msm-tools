package com.valtech.aem.msmtools.core;

import com.day.cq.wcm.msm.api.LiveCopy;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.jcr.RangeIterator;
import org.apache.jackrabbit.commons.iterator.RangeIteratorAdapter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.mockito.Mockito;

public class MsmHelper {
  public static LiveRelationship createLiveRelationship(String sourcePath) {
    return createLiveRelationship(true, sourcePath, false, "", true, false, true, null);
  }

  public static LiveRelationship createLiveRelationship(String sourcePath, boolean isRoot) {
    return createLiveRelationship(true, sourcePath, false, "", isRoot, false, true, null);
  }

  public static LiveRelationship createLiveRelationship(String sourcePath, String targetPath, boolean isRoot) {
    return createLiveRelationship(true, sourcePath, true, targetPath, isRoot, false, true, null);
  }

  public static LiveRelationship createLiveRelationship(
      boolean sourceExists,
      String sourcePath,
      boolean targetExists,
      String targetPath,
      boolean isCancelled,
      Date lastRolledOut
  ) {
    return createLiveRelationship(sourceExists, sourcePath, targetExists, targetPath, false, isCancelled, true, lastRolledOut);
  }

  public static LiveRelationship createLiveRelationship(ResourceResolver resourceResolver, String sourcePath, String targetPath) {
    Resource targetResource = resourceResolver.getResource(targetPath);
    List<String> targetMixins = getMixins(resourceResolver, targetPath);
    return createLiveRelationship(
        resourceResolver.getResource(sourcePath) != null,
        sourcePath,
        targetResource != null,
        targetPath,
        targetMixins.contains("cq:LiveSync"),
        targetMixins.contains("cq:LiveSyncCancelled"),
        targetResource != null ? targetResource.getResourceType().equals("cq:Page") : false,
        targetResource != null ? targetResource.getValueMap().get("jcr:content/cq:lastRolledout", Date.class) : null
    );

  }

  public static LiveRelationship createLiveRelationship(
      boolean sourceExists,
      String sourcePath,
      boolean targetExists,
      String targetPath,
      boolean isRoot,
      boolean isCancelled,
      boolean isPage,
      Date lastRolledOut
      ) {
    LiveCopy liveCopy = Mockito.mock(LiveCopy.class);
    Mockito.when(liveCopy.isRoot()).thenReturn(isRoot);

    LiveStatus liveStatus = Mockito.mock(LiveStatus.class);
    Mockito.when(liveStatus.isSourceExisting()).thenReturn(sourceExists);
    Mockito.when(liveStatus.isTargetExisting()).thenReturn(targetExists);
    Mockito.when(liveStatus.isCancelled()).thenReturn(isCancelled);
    Mockito.when(liveStatus.isPage()).thenReturn(isPage);
    Mockito.when(liveStatus.getLastRolledOut()).thenReturn(lastRolledOut);

    LiveRelationship liveRelationship = Mockito.mock(LiveRelationship.class);
    Mockito.when(liveRelationship.getSourcePath()).thenReturn(sourcePath);
    Mockito.when(liveRelationship.getTargetPath()).thenReturn(targetPath);
    Mockito.when(liveRelationship.getLiveCopy()).thenReturn(liveCopy);
    Mockito.when(liveRelationship.getStatus()).thenReturn(liveStatus);
    return liveRelationship;
  }

  public static List<String> getMixins(ResourceResolver resourceResolver, String pagePath) {
    Resource res = resourceResolver.getResource(pagePath);
    String[] mixins = new String[0];
    if (res != null) {
      mixins = res.getValueMap().get("jcr:content/jcr-mixinTypes", new String[0]);
    }
    return Arrays.asList(mixins);
  }
}
