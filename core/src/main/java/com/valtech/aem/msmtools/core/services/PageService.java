package com.valtech.aem.msmtools.core.services;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMException;
import com.valtech.aem.msmtools.core.helpers.PublicationStatus;
import java.util.Date;
import java.util.List;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public interface PageService {

  PublicationStatus getPublicationStatus(Resource pageResource);

  String getRegion(String pagePath);

  String getLanguage(String pagePath);

  String getVersionId(String pagePath, Date fromDate, Date toDate, ResourceResolver resourceResolver);

  Date createVersion(Page page, String label, String comment) throws WCMException;

  List<String> getMixins(String pagePath, ResourceResolver resourceResolver);

}
