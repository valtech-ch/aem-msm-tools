package com.valtech.aem.msmtools.core.services.impl;

import com.day.cq.commons.Externalizer;
import com.day.cq.mailer.MailingException;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.valtech.aem.msmtools.core.models.LiveCopyManagerDataSource;
import com.valtech.aem.msmtools.core.services.BlueprintService;
import com.valtech.aem.msmtools.core.services.MailService;
import com.valtech.aem.msmtools.core.services.NotificationService;
import com.valtech.aem.msmtools.core.services.impl.NotificationServiceImpl.Configuration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Slf4j
@Component(immediate = true, service = NotificationService.class)
@ServiceDescription("Valtech - MSM Tools - Notification Service")
@Designate(ocd = Configuration.class)
public class NotificationServiceImpl implements NotificationService {

  public static final String EMAIL_CONTENT_PLACEHOLDER = "{liveCopyManagerLink}";
  public static final String DEFAULT_SUBJECT = "Website - New content ready";
  public static final String DEFAULT_EMAIL_CONTENT =
      "The blueprint content for the following live copy page has changed:\n"
          + EMAIL_CONTENT_PLACEHOLDER + "\n"
          + "Please update your local pages to reflect the newest changes.";
  public static final String DEFAULT_SITE_OWNER_EMAIL_PAGE_PROP = "siteOwnerEmail";

  @ObjectClassDefinition(name = "Valtech - MSM Tools - Notification Service Config")
  public @interface Configuration {

    @AttributeDefinition(
        name = "Default E-mail Subject",
        description = "Will be used if the editor did not provide a custom notification e-mail subject.",
        type = AttributeType.STRING)
    String subject() default DEFAULT_SUBJECT;

    @AttributeDefinition(
        name = "Default E-mail Content",
        description = "Will be used if the editor did not provide a custom notification e-mail subject."
            + "Available placeholder: " + EMAIL_CONTENT_PLACEHOLDER + "  - will be replaced with link to live copy manager",
        type = AttributeType.STRING)
    String emailContent() default DEFAULT_EMAIL_CONTENT;

    @AttributeDefinition(
        name = "Site Owner E-mail Page Property Name",
        description = "Page property where site owner e-mails are stored (may be of type String[]), "
            + "to which the notification e-mails will be sent.",
        type = AttributeType.STRING)
    String siteOwnerEmailPageProperty() default DEFAULT_SITE_OWNER_EMAIL_PAGE_PROP;
  }

  @Reference
  private BlueprintService blueprintService;

  @Reference
  private MailService mailService;

  @Reference
  private Externalizer externalizer;

  private Configuration config;

  @Activate
  void activate(Configuration configuration) {
    this.config = configuration;
  }

  @Override
  public List<String> sendEmail(String subject, String emailContent, List<String> liveCopyPaths, ResourceResolver resourceResolver) {
    List<String> messages = new ArrayList<>();
    if (StringUtils.isBlank(subject)) {
      subject = config.subject();
    }
    if (StringUtils.isBlank(emailContent)) {
      emailContent = config.emailContent();
    }
    for (String liveCopyPath : liveCopyPaths) {
      Resource liveCopy = resourceResolver.getResource(liveCopyPath);
      if (liveCopy == null || ResourceUtil.isNonExistingResource(liveCopy)) {
        messages.add("Could not find " + liveCopyPath);
        continue;
      }
      Resource blueprint = blueprintService.getBlueprint(liveCopy);
      if (blueprint == null || ResourceUtil.isNonExistingResource(blueprint)) {
        messages.add("Could not find blueprint for " + liveCopyPath);
        continue;
      }
      try {
        String[] from = getEmailForContext(blueprint);
        String[] to = getEmailForContext(liveCopy);
        if (ArrayUtils.isEmpty(from)) {
          messages.add("No sender e-mail provided for " + blueprint.getPath());
          continue;
        }
        if (ArrayUtils.isEmpty(to)) {
          messages.add("No receiver e-mail provided for " + liveCopy.getPath());
          continue;
        }
        String liveCopyManagerLink = externalizer
            .authorLink(resourceResolver, LiveCopyManagerDataSource.PAGE_PATH + liveCopyPath);
        mailService
            .send(from[0], to, subject, prepareEmailContent(emailContent, liveCopyManagerLink));
      } catch (EmailException | MailingException e) {
        messages.add("Could not send notification for " + liveCopyPath);
        log.error("Could not send notification for " + liveCopyPath, e);
        continue;
      }
    }
    return messages;
  }

  private String prepareEmailContent(String template, String liveCopyManagerLink) {
    StringBuilder stringBuilder = new StringBuilder();
    String[] lines = StringUtils.trimToEmpty(template).split("\\r?\\n");
    stringBuilder.append("<html><body>");
    for (String line : lines) {
      stringBuilder.append("<p>").append(line).append("</p>");
    }
    stringBuilder.append("</body></html>");
    String linkPlaceholderValue = String.format("<a href=\"%s\">%s</a>", liveCopyManagerLink, liveCopyManagerLink);
    return stringBuilder.toString().replace(EMAIL_CONTENT_PLACEHOLDER, linkPlaceholderValue);
  }

  private String[] getEmailForContext(Resource contextResource) {
    PageManager pageManager = contextResource.getResourceResolver().adaptTo(PageManager.class);
    Page page = pageManager.getContainingPage(contextResource);
    return page.getProperties().get(config.siteOwnerEmailPageProperty(), String[].class);
  }
}
