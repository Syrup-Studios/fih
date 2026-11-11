package net.syrupstudios.fih;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.syrupstudios.fih.menu.TackleMenu;

public final class FihMenus {
    public static final MenuType<TackleMenu> TACKLE = new MenuType<>(TackleMenu::client, FeatureFlags.DEFAULT_FLAGS);
    private FihMenus() {}
}
