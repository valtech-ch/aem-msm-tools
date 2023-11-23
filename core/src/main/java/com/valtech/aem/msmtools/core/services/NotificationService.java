package com.valtech.aem.msmtools.core.services;

import java.util.List;
import org.apache.sling.api.resource.ResourceResolver;

public interface NotificationService {
  List<String> sendEmail(String subject, String emailContent, List<String> liveCopyPaths, ResourceResolver resourceResolver);
}
