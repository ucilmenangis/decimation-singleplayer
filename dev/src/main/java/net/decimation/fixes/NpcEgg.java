package net.decimation.fixes;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Facing;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/**
 * One spawn egg per NPC tier (NpcLoadouts): metadata = index in its tier
 * list, so every tier added later (juggernaut...) gets an egg on its own.
 * Spawns the NPC with that tier's gear, gun and health. Looks like a vanilla
 * spawn egg: base colour by faction, spots by tier. Creative tab Misc.
 */
public class NpcEgg extends Item
{
    private final NpcLoadouts loadouts;
    @SideOnly(Side.CLIENT)
    private IIcon overlay;

    public NpcEgg(NpcLoadouts loadouts)
    {
        this.loadouts = loadouts;
        setHasSubtypes(true);
        setUnlocalizedName("deciworldgen.npcEgg");
        setCreativeTab(CreativeTabs.tabMisc);
    }

    private NpcLoadouts.Tier tier(ItemStack stack)
    {
        int i = stack.getItemDamage();
        return i >= 0 && i < loadouts.tiers.size() ? loadouts.tiers.get(i) : null;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack)
    {
        NpcLoadouts.Tier t = tier(stack);
        return t == null ? "Spawn NPC" : "Spawn " + label(t);
    }

    /** "Bandit (heavy)", "Soldier (marineforest)", "Military". */
    static String label(NpcLoadouts.Tier t)
    {
        int u = t.name.indexOf('_');
        String head = u < 0 ? t.name : t.name.substring(0, u);
        head = Character.toUpperCase(head.charAt(0)) + head.substring(1);
        return u < 0 ? head : head + " (" + t.name.substring(u + 1) + ")";
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
                             float hx, float hy, float hz)
    {
        if (world.isRemote)
        {
            return true;
        }
        NpcLoadouts.Tier t = tier(stack);
        if (t == null)
        {
            return false;
        }
        x += Facing.offsetsXForSide[side];
        y += Facing.offsetsYForSide[side];
        z += Facing.offsetsZForSide[side];
        EntityLiving npc = t.kind == NpcKind.SOVIET ? Deci.newSoviet(world)
            : t.kind == NpcKind.SOLDIER ? Deci.newSoldier(world) : Deci.newBandit(world);
        loadouts.equip(npc, t, npc.getEntityData());
        npc.setLocationAndAngles(x + 0.5, y, z + 0.5, player.rotationYaw + 180, 0);
        npc.rotationYawHead = npc.renderYawOffset = npc.rotationYaw;
        world.spawnEntityInWorld(npc);
        if (!player.capabilities.isCreativeMode)
        {
            stack.stackSize--;
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void getSubItems(Item item, CreativeTabs tab, List list)
    {
        for (int i = 0; i < loadouts.tiers.size(); i++)
        {
            list.add(new ItemStack(item, 1, i));
        }
    }

    // ---- looks: vanilla spawn egg, two colours

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister reg)
    {
        itemIcon = reg.registerIcon("spawn_egg");
        overlay = reg.registerIcon("spawn_egg_overlay");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses()
    {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamageForRenderPass(int meta, int pass)
    {
        return pass > 0 ? overlay : itemIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass)
    {
        NpcLoadouts.Tier t = tier(stack);
        if (t == null)
        {
            return 0xFFFFFF;
        }
        // base: Decimation's own egg colours per faction; spots: the tier
        int base = t.kind == NpcKind.SOVIET ? 0xCCB400 : t.kind == NpcKind.SOLDIER ? 0x3381A3 : 0xEAC29A;
        if (pass == 0)
        {
            return base;
        }
        String n = t.name;
        return n.endsWith("light") ? 0x7A5230 : n.endsWith("medium") ? 0x556B2F : n.endsWith("heavy") ? 0x1E1E1E
            : n.endsWith("forest") ? 0x2F4F2F : n.endsWith("urban") ? 0x9A9A9A : n.endsWith("black") ? 0x111111
            : n.equals("military") ? 0x7F0000 : n.endsWith("rpg") ? 0xD2691E : n.equals("juggernaut") ? 0x333333
            : n.equals("juggernaut_sniper") ? 0x6B6B6B : n.equals("elite_military") ? 0x101010
            : n.equals("elite_sniper") ? 0x2E8B57
            : 0xA09159;
    }
}
