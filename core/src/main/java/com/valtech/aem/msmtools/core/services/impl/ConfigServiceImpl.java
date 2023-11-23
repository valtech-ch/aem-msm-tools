package com.valtech.aem.msmtools.core.services.impl;

import com.valtech.aem.msmtools.core.services.ConfigService;
import com.valtech.aem.msmtools.core.services.impl.ConfigServiceImpl.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Slf4j
@Component(immediate = true, service = ConfigService.class)
@ServiceDescription("Valtech - MSM Tools - Config Service")
@Designate(ocd = Configuration.class)
public class ConfigServiceImpl implements ConfigService {

  @ObjectClassDefinition(name = "Valtech - MSM Tools - Config Service Config")
  public @interface Configuration {

    @AttributeDefinition(
        name = "Blueprint Root Max Depth",
        description = "How deep the page tree traversal will go to look for blueprint sites",
        type = AttributeType.INTEGER)
    int blueprintRootMaxDepth() default 3;

    @AttributeDefinition(
        name = "Resolve Nested Live Copies",
        description = "If the blueprint manager should list nested live copies (live copies of live copies)",
        type = AttributeType.BOOLEAN)
    boolean resolveNestedLiveCopies() default true;

    @AttributeDefinition(
        name = "Region / Country page depth",
        description = "At which level the country/region page is in the page tree structure. /content is considered depth 0",
        type = AttributeType.INTEGER)
    int regionPageDepth() default 2;

    @AttributeDefinition(
        name = "Language page depth",
        description = "At which level the language page is in the page tree structure. /content is considered depth 0",
        type = AttributeType.INTEGER)
    int languagePageDepth() default 3;

    @AttributeDefinition(
        name = "Allow e-mail notification action",
        description = "Show a notification action in the blueprint manager, which allows to "
            + "send e-mails to live copy site owners",
        type = AttributeType.BOOLEAN)
    boolean allowEmailNotificationsAction() default false;

    @AttributeDefinition(
        name = "Allow review action",
        description = "Once a page is updated from the blueprint, an additional action becomes available to confirm"
            + "that it was also reviewed and any potential manual customization done.",
        type = AttributeType.BOOLEAN)
    boolean allowMarkAsDoneAction() default false;

    @AttributeDefinition(
        name = "Allow version comparison action",
        description = "Show an action, which would allow the editor to see which changes were done on the blueprint "
            + "page since the last rollout",
        type = AttributeType.BOOLEAN)
    boolean allowVersionCompareAction() default true;

    @AttributeDefinition(
        name = "Blueprint page must be published",
        description = "Treat live copy pages as up-to-date, if the blueprint page is not published, or it was not "
            + "published since the last modification.",
        type = AttributeType.BOOLEAN)
    boolean blueprintPagesMustBePublished() default false;

    @AttributeDefinition(
        name = "Show all pages in live copy manager",
        description = "Disabling this option allows to show only those live copy page, which need "
            + "attention by the editors",
        type = AttributeType.BOOLEAN)
    boolean showAllPagesInLiveCopyManager() default true;

    @AttributeDefinition(
        name = "Page Move Rollout Action Used",
        description = "On moved blueprint page rollout, live copy pages are moved as well. This depends on your "
            + "configured live copy rollout actions.",
        type = AttributeType.BOOLEAN)
    boolean pageMoveRolloutActionUsed() default true;
  }

  private Configuration config;

  @Activate
  void activate(Configuration configuration) {
    this.config = configuration;
  }

  @Override
  public int getBlueprintRootMaxDepth() {
    return config.blueprintRootMaxDepth();
  }

  @Override
  public int getRegionPageDepth() {
    return config.regionPageDepth();
  }

  @Override
  public int getLanguagePageDepth() {
    return config.languagePageDepth();
  }

  @Override
  public boolean resolveNestedLiveCopies() {
    return config.resolveNestedLiveCopies();
  }

  @Override
  public boolean allowEmailNotificationAction() {
    return config.allowEmailNotificationsAction();
  }

  @Override
  public boolean allowMarkAsDoneAction() {
    return config.allowMarkAsDoneAction();
  }

  @Override
  public boolean allowVersionCompareAction() {
    return config.allowVersionCompareAction();
  }

  @Override
  public boolean blueprintPagesMustBePublished() {
    return config.blueprintPagesMustBePublished();
  }

  @Override
  public boolean showAllPagesInLiveCopyManager() {
    return config.showAllPagesInLiveCopyManager();
  }

  @Override
  public boolean pageMoveRolloutActionUsed() {
    return config.pageMoveRolloutActionUsed();
  }
}
