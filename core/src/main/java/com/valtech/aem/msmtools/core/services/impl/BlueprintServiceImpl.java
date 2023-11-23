package com.valtech.aem.msmtools.core.services.impl;

import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.valtech.aem.msmtools.core.services.BlueprintService;
import com.valtech.aem.msmtools.core.services.ConfigService;
import java.util.ArrayList;
import java.util.List;
import javax.jcr.RangeIterator;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;

@Slf4j
@Component(immediate = true, service = BlueprintService.class)
@ServiceDescription("Valtech - MSM Tools - Blueprint Service")
public class BlueprintServiceImpl implements BlueprintService {

  @Reference
  private LiveRelationshipManager liveRelationshipManager;

  @Reference
  private ConfigService configService;

  @Override
  public Resource getBlueprint(Resource liveCopy) {
    try {
      LiveRelationship liveRelationship = liveRelationshipManager.getLiveRelationship(liveCopy, false);
      if (liveRelationship == null) {
        return null;
      }
      return liveCopy.getResourceResolver().getResource(liveRelationship.getSourcePath());
    } catch (WCMException e) {
      log.warn("Could not fetch live relationship for path " + liveCopy.getPath());
      return null;
    }
  }

  @Override
  public List<Resource> getBlueprints(Resource blueprintResource) {
    List<Resource> liveRelationships = new ArrayList<>();
    if (blueprintResource == null) {
      return liveRelationships;
    }
    try {

      // resolve the live copies for the given resource:
      RangeIterator rangeIterator = liveRelationshipManager.getLiveRelationships(blueprintResource, null, null);
      if (rangeIterator.hasNext()) {
        // we're interested to know if the resource is a blueprint for at least one live copy,
        LiveRelationship lr = (LiveRelationship) rangeIterator.next();
        if (lr.getLiveCopy() != null && lr.getLiveCopy().isRoot()) {
          liveRelationships.add(blueprintResource);
          // if the resource is a blueprint root - there's no point in going any deeper, hence return:
          return liveRelationships;
        }
      }

      // if the given resource has no live copies, but the lookup depth is not exhausted, go deeper down the tree:
      if (blueprintResource.getPath().substring(1).split("/").length - 1 < configService.getBlueprintRootMaxDepth()) {
        blueprintResource.listChildren().forEachRemaining(
            child -> liveRelationships.addAll(getBlueprints(child))
        );
      }
    } catch (WCMException e) {
      log.error("Error occurred when getting blueprint relationships for resource: " + blueprintResource.getPath());
    }
    return liveRelationships;
  }

  @Override
  public List<LiveRelationship> getRelationships(Resource blueprintResource) {
    List<LiveRelationship> liveRelationships = new ArrayList<>();
    if (blueprintResource == null) {
      return liveRelationships;
    }
    try {
      // resolve the live copies for the given resource:
      RangeIterator rangeIterator = liveRelationshipManager.getLiveRelationships(blueprintResource, null, null);
      rangeIterator.forEachRemaining(item -> {
        LiveRelationship lr = (LiveRelationship) item;
        if (lr.getLiveCopy() != null && lr.getLiveCopy().isRoot()) {
          liveRelationships.add(lr);
        }
      });

      // if live copies are of more than one level - these will be added here:
      if (configService.resolveNestedLiveCopies()) {
        List<LiveRelationship> nextLevelRelationships = new ArrayList<>();
        liveRelationships.forEach(liveRelationship -> {
          Resource blueprint = blueprintResource.getResourceResolver().getResource(liveRelationship.getTargetPath());
          nextLevelRelationships.addAll(getRelationships(blueprint));
        });
        liveRelationships.addAll(nextLevelRelationships);
      }
    } catch (WCMException e) {
      log.error("Error occurred when getting blueprint relationships for resource: " + blueprintResource.getPath());
    }
    return liveRelationships;
  }

}
