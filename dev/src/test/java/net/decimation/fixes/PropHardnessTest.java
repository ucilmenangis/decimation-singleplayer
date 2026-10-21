package net.decimation.fixes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PropHardnessTest
{
    @Test
    public void aDecimationBlockWithoutHardnessGetsOne()
    {
        assertTrue(PropHardness.needs("deci:BlockStreetBarrier", 0f));
        assertTrue(PropHardness.needs("deci:BlockElevator", 0f));
        assertTrue(PropHardness.needs("deciworldgen:anything", 0f));
    }

    @Test
    public void ropesBreakOnPurpose()
    {
        assertFalse(PropHardness.needs("deci:BlockRope", 0f));
        assertFalse(PropHardness.needs("deci:BlockRopeTop", 0f));
    }

    @Test
    public void otherBlocksAndSetHardnessesAreLeftAlone()
    {
        assertFalse(PropHardness.needs("minecraft:tallgrass", 0f));
        assertFalse(PropHardness.needs("deci:BlockSandbagStack", 0.4f));
        assertFalse(PropHardness.needs("deci:BlockMapBarrier", -1f));
    }
}
