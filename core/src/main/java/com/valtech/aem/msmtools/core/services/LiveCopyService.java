package com.valtech.aem.msmtools.core.services;

import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.valtech.aem.msmtools.core.helpers.LiveCopyFilter;
import com.valtech.aem.msmtools.core.helpers.LiveCopyStatus;
import java.util.List;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public interface LiveCopyService {

  LiveCopyFilter getListFilter(boolean showAllPages);

  List<LiveRelationship> getRelationships(Resource resource, LiveCopyFilter filter);

  LiveCopyStatus getStatus(LiveRelationship liveRelationship, ResourceResolver resourceResolver);

  void rollout(String liveCopyRoot, String[] paths, ResourceResolver resourceResolver)
      throws WCMException, PersistenceException;

  void removeLastRolloutDate(String pagePath, ResourceResolver resourceResolver) throws PersistenceException;

}
