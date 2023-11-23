package com.valtech.aem.msmtools.core.servlets;

import com.day.cq.wcm.msm.api.LiveRelationship;
import com.google.gson.Gson;
import com.valtech.aem.msmtools.core.helpers.LiveCopyStatus;
import com.valtech.aem.msmtools.core.services.LiveCopyService;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.jcr.query.Query;
import javax.servlet.Servlet;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
    service = Servlet.class,
    property = {
        org.osgi.framework.Constants.SERVICE_DESCRIPTION + "=MSM Check Status Servlet"
    }
)
@SlingServletPaths("/bin/valtech/msm-tools/check-status")
@SlingServletResourceTypes(
    resourceTypes = "sling/unused",
    methods = HttpConstants.METHOD_GET
)
@Slf4j
public class CheckStatusServlet extends SlingAllMethodsServlet {

  @Reference
  private transient LiveCopyService liveCopyService;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
    String path = request.getParameter("path");
    if (StringUtils.isBlank(path)) {
      log.info("No source path was given");
      return;
    }
    Resource liveCopyResource = request.getResourceResolver().getResource(path);
    if (liveCopyResource == null) {
      log.info("Resource for given path " + path + " was not found");
      return;
    }

    // calculate state:
    List<LiveRelationship> liveRelationships = liveCopyService.getRelationships(
        liveCopyResource,
        liveCopyService.getListFilter(false));
    JsonResponse json = new JsonResponse();
    if (!liveRelationships.isEmpty()) {
      String status = String.format("%s (%s)", LiveCopyStatus.OUTDATED.getDisplayName(), liveRelationships.size());
      json.setState(status);
    } else {
      json.setState(LiveCopyStatus.UP_TO_DATE.getDisplayName());
    }

    // fetch last update time:
    String query = String.format("SELECT * FROM [cq:Page] AS s "
        + "WHERE ISDESCENDANTNODE([%s]) "
        + "AND s.[jcr:content/cq:lastRolledout] IS NOT NULL "
        + "ORDER BY s.[jcr:content/cq:lastRolledout] DESC", path);
    Iterator<Resource> resources = request.getResourceResolver().findResources(query, Query.JCR_SQL2);
    if (resources.hasNext()) {
      Date lastRolledOutDate = resources.next().getValueMap().get("jcr:content/cq:lastRolledout", Date.class);
      if (lastRolledOutDate != null) {
        json.setLastUpdated(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(lastRolledOutDate));
      }
    }
    // write response:
    response.getWriter().write(new Gson().toJson(json));
  }

  @Setter
  public class JsonResponse {
    String state;
    String lastUpdated;
  }
}