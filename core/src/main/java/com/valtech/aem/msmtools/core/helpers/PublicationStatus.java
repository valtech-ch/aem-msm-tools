package com.valtech.aem.msmtools.core.helpers;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum PublicationStatus {
    UNKNOWN("unknown","Unknown"), // page is published
    NOT_PUBLISHED("notPublished","Not published"), // page is published
    MODIFIED("modified","Modified"), // page is modified
    PUBLISHED("published", "Published"); // page is not published

    @Getter
    private String status;

    @Getter
    private String displayName;

}
