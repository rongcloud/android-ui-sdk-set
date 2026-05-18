package io.rong.imkit.config;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class FeatureConfigTest {
    private static final java.util.List<String> DEFAULT_QUOTE_TYPES =
            Arrays.asList(
                    "RC:TxtMsg",
                    "RC:ImgMsg",
                    "RC:GIFMsg",
                    "RC:SightMsg",
                    "RC:VcMsg",
                    "RC:HQVCMsg",
                    "RC:FileMsg",
                    "RC:LBSMsg");

    @Test
    public void quoteMessageTypeWhiteListDefaultsToAllBuiltInQuoteTypes() {
        FeatureConfig config = new FeatureConfig();

        assertEquals(DEFAULT_QUOTE_TYPES, config.getQuoteMessageTypeWhiteList());
    }

    @Test
    public void setQuoteMessageTypeWhiteListKeepsEmptySelection() {
        FeatureConfig config = new FeatureConfig();

        config.setQuoteMessageTypeWhiteList(Collections.emptyList());

        assertEquals(Collections.emptyList(), config.getQuoteMessageTypeWhiteList());
    }

    @Test
    public void setQuoteMessageTypeWhiteListResetsNullToDefault() {
        FeatureConfig config = new FeatureConfig();
        config.setQuoteMessageTypeWhiteList(Collections.emptyList());

        config.setQuoteMessageTypeWhiteList(null);

        assertEquals(DEFAULT_QUOTE_TYPES, config.getQuoteMessageTypeWhiteList());
    }
}
