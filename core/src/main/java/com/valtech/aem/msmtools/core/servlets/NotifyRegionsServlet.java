package com.valtech.aem.msmtools.core.servlets;

import com.valtech.aem.msmtools.core.services.ConfigService;
import com.valtech.aem.msmtools.core.services.NotificationService;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.Servlet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpStatus;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
    service = Servlet.class,
    property = {
        org.osgi.framework.Constants.SERVICE_DESCRIPTION + "=MSM Notify Regions Servlet"
    }
)
@SlingServletPaths("/bin/valtech/msm-tools/notify-regions")
@SlingServletResourceTypes(
    resourceTypes = "sling/unused",
    methods = HttpConstants.METHOD_POST
)
@Slf4j
public class NotifyRegionsServlet extends SlingAllMethodsServlet {

  @Reference
  private transient NotificationService notificationService;

  @Reference
  private transient ConfigService configService;

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {
    if (!configService.allowEmailNotificationAction()) {
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return;
    }
    String subject = request.getParameter("subject");
    String emailContent = request.getParameter("emailContent");
    RequestParameter[] requestParameters = request.getRequestParameters("liveCopyPaths");
    List<String> liveCopyPaths = ArrayUtils.isEmpty(requestParameters) ? Collections.emptyList()
        : Stream.of(requestParameters).map(RequestParameter::getString).collect(Collectors.toList());
    if (liveCopyPaths.isEmpty()) {
      response.setStatus(HttpStatus.SC_BAD_REQUEST);
      return;
    }
    List<String> messages = notificationService.sendEmail(subject, emailContent, liveCopyPaths, request.getResourceResolver());
    if (!messages.isEmpty()) {
      sendResponse(response, messages, HttpStatus.SC_INTERNAL_SERVER_ERROR);
    } else {
      sendResponse(response, null, HttpStatus.SC_OK);
    }
  }

  private static void sendResponse(SlingHttpServletResponse response, List<String> messages, int status) {
    try {
      if (messages != null) {
        PrintWriter writer = response.getWriter();
        for (String message : messages) {
          writer.write("<p>" + message + "</p>");
        }
        response.setContentType("text/html");
      }
      response.setStatus(status);
    } catch (IOException e) {
      response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }
  }
}