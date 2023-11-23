package com.valtech.aem.msmtools.core.services.impl;

import com.valtech.aem.msmtools.core.services.ConfigService;
import com.valtech.aem.msmtools.core.services.PageService;
import com.valtech.aem.msmtools.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junit.framework.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class, AemContextExtension.class})
class PageServiceImplTest {

  private AemContext context = AppAemContext.newAemContextBuilder().build();

  private PageService pageService;

  @Mock
  private ConfigService configService;

  @BeforeEach
  void setUp() {
    context.registerService(ConfigService.class, configService);
    pageService = context.registerInjectActivateService(PageServiceImpl.class);
  }

  @Test
  void getRegion() {
    Mockito.doReturn(2).when(configService).getRegionPageDepth();
    Assert.assertEquals("two", pageService.getRegion("/content/one/two/tree"));
    Mockito.doReturn(3).when(configService).getRegionPageDepth();
    Assert.assertEquals("tree", pageService.getRegion("/content/one/two/tree"));
    Mockito.doReturn(4).when(configService).getRegionPageDepth();
    Assert.assertEquals("", pageService.getRegion("/content/one/two/tree"));
  }

  @Test
  void getLanguage() {
    Mockito.doReturn(2).when(configService).getLanguagePageDepth();
    Assert.assertEquals("two", pageService.getLanguage("/content/one/two/tree"));
    Mockito.doReturn(3).when(configService).getLanguagePageDepth();
    Assert.assertEquals("tree", pageService.getLanguage("/content/one/two/tree"));
    Mockito.doReturn(4).when(configService).getLanguagePageDepth();
    Assert.assertEquals("", pageService.getLanguage("/content/one/two/tree"));
  }
}