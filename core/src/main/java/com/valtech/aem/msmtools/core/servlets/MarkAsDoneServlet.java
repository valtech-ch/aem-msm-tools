package com.valtech.aem.msmtools.core.servlets;

import com.valtech.aem.msmtools.core.services.LiveCopyService;
import com.valtech.aem.msmtools.core.services.ConfigService;
import javax.servlet.Servlet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpStatus;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
    service = Servlet.class,
    property = {
        org.osgi.framework.Constants.SERVICE_DESCRIPTION + "=MSM Mark As Done Servlet"
    }
)
@SlingServletPaths("/bin/valtech/msm-tools/mark-as-done")
@SlingServletResourceTypes(
    resourceTypes = "sling/unused",
    methods = HttpConstants.METHOD_POST
)
@Slf4j
public class MarkAsDoneServlet extends SlingAllMethodsServlet {

  @Reference
  private transient ConfigService configService;

  @Reference
  private transient LiveCopyService liveCopyService;

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {
    if (!configService.allowMarkAsDoneAction()) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }
    String[] paths = request.getParameterValues("paths");
    if (ArrayUtils.isEmpty(paths)) {
      response.setStatus(HttpStatus.SC_BAD_REQUEST);
      return;
    }
    for (int i = 0; i < paths.length; i++) {
      try {
        liveCopyService.removeLastRolloutDate(paths[i], request.getResourceResolver());
      } catch (PersistenceException e) {
        log.error("Could remove last rollout date for " + paths[i], e);
        throw new RuntimeException(e);
      }
    }
  }
}