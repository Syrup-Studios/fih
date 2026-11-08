package net.syrupstudios.fih;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Fih {
    public static final Logger LOGGER = LoggerFactory.getLogger(ModIdentity.MOD_ID);

    private Fih() {}

    public static void initialize(String loader) {
        FihGameRules.initialize();
        LOGGER.info("{} initialized on {}", ModIdentity.MOD_NAME, loader);
    }
}
