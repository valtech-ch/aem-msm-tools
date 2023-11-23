package com.valtech.aem.msmtools.core.services;

import com.day.cq.wcm.msm.api.LiveRelationship;
import java.util.List;
import org.apache.sling.api.resource.Resource;

public interface BlueprintService {

  Resource getBlueprint(Resource liveCopy);

  List<Resource> getBlueprints(Resource blueprintResource);

  List<LiveRelationship> getRelationships(Resource blueprintResource);
}
