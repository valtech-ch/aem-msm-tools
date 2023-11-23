package com.valtech.aem.msmtools.core.renderconditions;

import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;
import com.valtech.aem.msmtools.core.services.ConfigService;
import javax.servlet.Servlet;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Slf4j
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "valtech-msm-tools/rendercondition/version-compare-action",
    methods = HttpConstants.METHOD_GET)
public class VersionCompareActionRenderCondition extends SlingSafeMethodsServlet {

  @Reference
  private transient ConfigService configService;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
    request.setAttribute(
        RenderCondition.class.getName(),
        new SimpleRenderCondition(configService.allowVersionCompareAction())
    );
  }
}
