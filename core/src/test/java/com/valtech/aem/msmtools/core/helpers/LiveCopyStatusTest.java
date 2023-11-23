package com.valtech.aem.msmtools.core.helpers;

import junit.framework.Assert;
import org.junit.jupiter.api.Test;

class LiveCopyStatusTest {

  @Test
  void forStatus() {
    Assert.assertNull(LiveCopyStatus.forStatus(null));
    Assert.assertNull(LiveCopyStatus.forStatus(""));
    Assert.assertNull(LiveCopyStatus.forStatus("asdf"));
    Assert.assertEquals(LiveCopyStatus.UP_TO_DATE, LiveCopyStatus.forStatus("upToDate"));
  }
}