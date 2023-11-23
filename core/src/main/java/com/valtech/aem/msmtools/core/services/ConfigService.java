package com.valtech.aem.msmtools.core.services;

public interface ConfigService {

  int getBlueprintRootMaxDepth();

  int getRegionPageDepth();

  int getLanguagePageDepth();

  boolean resolveNestedLiveCopies();

  boolean allowEmailNotificationAction();

  boolean allowMarkAsDoneAction();

  boolean allowVersionCompareAction();

  boolean blueprintPagesMustBePublished();

  boolean showAllPagesInLiveCopyManager();

  boolean pageMoveRolloutActionUsed();
}
