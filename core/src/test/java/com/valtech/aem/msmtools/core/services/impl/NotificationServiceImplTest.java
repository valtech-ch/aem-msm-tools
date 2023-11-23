package com.valtech.aem.msmtools.core.services.impl;

import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.valtech.aem.msmtools.core.helpers.LiveCopyStatus;
import com.valtech.aem.msmtools.core.models.LiveCopyManagerDataSource;
import com.valtech.aem.msmtools.core.services.BlueprintService;
import com.valtech.aem.msmtools.core.services.MailService;
import com.valtech.aem.msmtools.core.services.NotificationService;
import com.valtech.aem.msmtools.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import junit.framework.Assert;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.verification.VerificationMode;

@ExtendWith({MockitoExtension.class, AemContextExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceImplTest {

  private AemContext context = AppAemContext.newAemContextBuilder().build();

  private NotificationService notificationService;

  @Mock private BlueprintService blueprintService;
  @Mock private MailService mailService;
  @Mock private Externalizer externalizer;

  @BeforeEach
  void setUp() {
    context.registerService(BlueprintService.class, blueprintService);
    context.registerService(MailService.class, mailService);
    context.registerService(Externalizer.class, externalizer);
    Map<String, Object> properties = new HashMap<>();
    properties.put("subject", NotificationServiceImpl.DEFAULT_SUBJECT);
    properties.put("emailContent", NotificationServiceImpl.DEFAULT_EMAIL_CONTENT);
    properties.put("siteOwnerEmailPageProperty", NotificationServiceImpl.DEFAULT_SITE_OWNER_EMAIL_PAGE_PROP);
    notificationService = context.registerInjectActivateService(new NotificationServiceImpl(), properties);
    context.load().json("/com/valtech/aem/msmtools/core/services/notification-sites.json", "/content");
    Mockito.doAnswer(invocationOnMock -> {
      switch(invocationOnMock.getArgument(0, Resource.class).getPath()) {
        case "/content/notification-sites/live-copies/no-blueprint": return null;
        case "/content/notification-sites/live-copies/no-owner": return context.resourceResolver().getResource("/content/notification-sites/blueprints/with-owner");
        case "/content/notification-sites/live-copies/with-owner": return context.resourceResolver().getResource("/content/notification-sites/blueprints/no-owner");
        default: return context.resourceResolver().getResource("/content/notification-sites/blueprints/multiple-owner");
      }
    }).when(blueprintService).getBlueprint(Mockito.any(Resource.class));
    Mockito.doAnswer(invocationOnMock -> invocationOnMock.getArgument(1, String.class))
        .when(externalizer).authorLink(Mockito.any(ResourceResolver.class), Mockito.anyString());
  }

  @Test
  void sendEmail() throws EmailException {
    Map<String, Object> mailDataMap = new HashMap<>();
    Mockito.doAnswer(invocationOnMock -> {
      Integer count = (Integer) mailDataMap.get("count");
      count++;
      mailDataMap.put("count", count);
      mailDataMap.put("sender", invocationOnMock.getArgument(0, String.class));
      mailDataMap.put("receiver", invocationOnMock.getArgument(1, String[].class));
      mailDataMap.put("subject", invocationOnMock.getArgument(2, String.class));
      mailDataMap.put("content", invocationOnMock.getArgument(3, String.class));
      return null;
    }).when(mailService).send(Mockito.anyString(), Mockito.any(String[].class), Mockito.anyString(), Mockito.anyString());

    mailDataMap.put("count", 0);
    List<String> messages = notificationService.sendEmail(null, null, Collections.EMPTY_LIST, context.resourceResolver());
    Assert.assertTrue("No message for no live copies", messages.isEmpty());
    Assert.assertEquals("No e-mails sent", 0, mailDataMap.get("count"));

    List<String> liveCopies = new ArrayList<>();
    liveCopies.add("/content/non-existing");
    messages = notificationService.sendEmail(null, null, liveCopies, context.resourceResolver());
    Assert.assertEquals("One message for not found live copy", 1, messages.size());
    Assert.assertTrue("Not found message", messages.get(0).startsWith("Could not find"));
    Assert.assertEquals("No e-mails sent", 0, mailDataMap.get("count"));

    liveCopies = new ArrayList<>();
    liveCopies.add("/content/notification-sites/live-copies/no-blueprint");
    messages = notificationService.sendEmail(null, null, liveCopies, context.resourceResolver());
    Assert.assertEquals("One message for not found blueprint", 1, messages.size());
    Assert.assertTrue("Not found blueprint message", messages.get(0).startsWith("Could not find blueprint"));
    Assert.assertEquals("No e-mails sent", 0, mailDataMap.get("count"));

    liveCopies = new ArrayList<>();
    liveCopies.add("/content/notification-sites/live-copies/no-owner");
    messages = notificationService.sendEmail(null, null, liveCopies, context.resourceResolver());
    Assert.assertEquals("One message for not found live copy owner", 1, messages.size());
    Assert.assertTrue("Not found message", messages.get(0).startsWith("No receiver"));
    Assert.assertEquals("No e-mails sent", 0, mailDataMap.get("count"));

    liveCopies = new ArrayList<>();
    liveCopies.add("/content/notification-sites/live-copies/with-owner");
    messages = notificationService.sendEmail(null, null, liveCopies, context.resourceResolver());
    Assert.assertEquals("One message for not found blueprint owner", 1, messages.size());
    Assert.assertTrue("Not found message", messages.get(0).startsWith("No sender"));
    Assert.assertEquals("No e-mails sent", 0, mailDataMap.get("count"));

    liveCopies = new ArrayList<>();
    liveCopies.add("/content/notification-sites/live-copies/multiple-owner");
    messages = notificationService.sendEmail(null, null, liveCopies, context.resourceResolver());
    Assert.assertEquals("No messages for proper sending", 0, messages.size());
    Assert.assertEquals("No messages for proper sending", "one@example.com", mailDataMap.get("sender"));
    String[] receiver = (String[]) mailDataMap.get("receiver");
    Assert.assertEquals("Correct receiver amount", 2, receiver.length);
    Assert.assertTrue("Correct first receiver mail", ArrayUtils.contains(receiver, "lc-one@example.com"));
    Assert.assertTrue("Correct second receiver mail", ArrayUtils.contains(receiver, "lc-two@example.com"));
    Assert.assertEquals("Correct subject", NotificationServiceImpl.DEFAULT_SUBJECT, mailDataMap.get("subject"));
    String content = (String) mailDataMap.get("content");
    Assert.assertTrue("Correct content start", StringUtils.contains(content, "The blueprint content for the following"));
    Assert.assertTrue("No placeholder present", !StringUtils.contains(content, NotificationServiceImpl.EMAIL_CONTENT_PLACEHOLDER));
    Assert.assertTrue("Live copy manager link present", StringUtils.contains(content, LiveCopyManagerDataSource.PAGE_PATH));
    Assert.assertTrue("Live copy path present", StringUtils.contains(content, "/content/notification-sites/live-copies/multiple-owner"));
    Assert.assertEquals("Mails sent with default subject and content", 1, mailDataMap.get("count"));

    mailDataMap.put("count", 0);
    notificationService.sendEmail("Custom subject", "Custom content " + NotificationServiceImpl.EMAIL_CONTENT_PLACEHOLDER, liveCopies, context.resourceResolver());
    Assert.assertEquals("Correct subject", "Custom subject", mailDataMap.get("subject"));
    content = (String) mailDataMap.get("content");
    Assert.assertTrue("Correct content start", StringUtils.contains(content, "Custom content"));
    Assert.assertTrue("No placeholder present", !StringUtils.contains(content, NotificationServiceImpl.EMAIL_CONTENT_PLACEHOLDER));
    Assert.assertTrue("Live copy manager link present", StringUtils.contains(content, LiveCopyManagerDataSource.PAGE_PATH));
    Assert.assertTrue("Live copy path present", StringUtils.contains(content, "/content/notification-sites/live-copies/multiple-owner"));
    Assert.assertEquals("Mails sent with custom subject and content", 1, mailDataMap.get("count"));

    mailDataMap.put("count", 0);
    liveCopies.add("/content/notification-sites/live-copies/multiple-owner");
    notificationService.sendEmail(null, null, liveCopies, context.resourceResolver());
    Assert.assertEquals("2 mails sent for 2 given live copies", 2, mailDataMap.get("count"));
  }
}