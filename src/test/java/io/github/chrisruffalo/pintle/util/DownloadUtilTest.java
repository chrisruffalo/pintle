package io.github.chrisruffalo.pintle.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

public class DownloadUtilTest {
    @Test
    public void keyValueString() {
        final Map<String, String> kvp = DownloadUtil.getKeysAndValuesFromString("max-age=300");
    }

}
