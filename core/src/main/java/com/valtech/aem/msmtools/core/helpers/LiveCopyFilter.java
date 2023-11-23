package com.valtech.aem.msmtools.core.helpers;

@FunctionalInterface
public interface LiveCopyFilter {
    boolean isValid(LiveCopyStatus liveCopyStatus);
}
