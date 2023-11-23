package com.valtech.aem.msmtools.core.helpers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor
public enum LiveCopyStatus {
    UP_TO_DATE("upToDate","Up to date", false), // page is up-to-date
    UP_TO_DATE_NOT_REVIEWED("upToDateNotReviewed","Up to date, not reviewed", false), // page is up-to-date, but not yet reviewed (also see configuration)
    OUTDATED("outdated", "Outdated", true), // is outdated
    NOT_READY("notReady", "Not Ready", false), // blueprint page is modified but not published yet (also see configuration)
    NEW_PAGE("newPage", "New", true), // new page added to blueprint but does not exist in live copy yet
    MOVED("moved", "Moved", true), // page was moved in the blueprint
    MOVED_OUTDATED("movedOutdated", "Moved / Outdated", true), // page was moved and updated in the blueprint
    SUSPENDED("suspended", "Suspended", false), // page updates have been suspended
    LOCAL_ONLY("localOnly", "Local Only", false), // page created and exists only in live copy
    UNKNOWN("unknown", "Unknown", false), // status unknown
    CONFLICTING("conflicting", "Conflicting", true), // new page under the same path added to blueprint and live copy independently
    DELETED("deleted", "Deleted", true); // page deleted in blueprint

    @Getter
    private String status;

    @Getter
    private String displayName;

    @Getter
    private boolean canBeRolledOut;

    public static LiveCopyStatus forStatus(String status) {
        for (LiveCopyStatus liveCopyStatus: values()) {
            if (StringUtils.equals(liveCopyStatus.getStatus(), status)) {
                return liveCopyStatus;
            }
        }
        return null;
    }
}
