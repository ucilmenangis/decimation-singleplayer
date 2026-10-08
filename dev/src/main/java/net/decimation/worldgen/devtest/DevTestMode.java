package net.decimation.worldgen.devtest;

import net.minecraft.client.Minecraft;

/**
 * One dev test mode run by {@link net.decimation.worldgen.DevAutoTest}: modes
 * of one launch run one after another in the same game session (one boot,
 * one world). Pick them with `./gradlew runClient -Ptest=checks,scope,...`
 * or `python3 tools/devtest.py checks scope ...`.
 */
public abstract class DevTestMode
{
    /** Name used in -Ptest=... and in the results file. */
    public abstract String name();

    /** True when the mode needs the fresh seed 1 world; else the last autotest world is reused. */
    public boolean freshWorld()
    {
        return false;
    }

    /** Client tick (END) while this mode is active; false once it is done. */
    public abstract boolean client(Minecraft mc);

    /** Server tick (END) while this mode is active. */
    public void server()
    {
    }
}
