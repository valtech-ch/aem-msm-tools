package com.valtech.aem.msmtools.core.servlets;

import com.day.cq.wcm.api.WCMException;
import com.valtech.aem.msmtools.core.services.LiveCopyService;
import java.io.IOException;
import javax.servlet.Servlet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
    service = Servlet.class,
    property = {Constants.SERVICE_DESCRIPTION + "=MSM Update Pages Servlet"}
)
@SlingServletPaths("/bin/valtech/msm-tools/synchronize")
@SlingServletResourceTypes(
    resourceTypes = "sling/unused",
    methods = HttpConstants.METHOD_POST
)
@Slf4j
public class SynchronizeServlet extends SlingAllMethodsServlet {

  @Reference
  private transient LiveCopyService liveCopyService;

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {
    String[] paths = request.getParameterValues("paths");
    String liveCopyRoot = request.getParameter("liveCopyRoot");
    if (paths == null || StringUtils.isBlank(liveCopyRoot)) {
      sendError(HttpStatus.SC_BAD_REQUEST, "Wrong parameters given", response);
      return;
    }
    try {
      liveCopyService.rollout(liveCopyRoot, paths, request.getResourceResolver());
    } catch (WCMException | PersistenceException e) {
      log.error("Could not properly rollout changes for live copy " + liveCopyRoot, e);
      sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Could not properly update", response);
      return;
    }
  }

  private void sendError(int statusCode, String message, SlingHttpServletResponse response) {
    try {
      response.setStatus(statusCode);
      response.getWriter().write(message);
    } catch (IOException e) {
      log.error("Could not write into the response", e);
    }
  }
}
