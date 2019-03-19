package kernitus.plugin.OldCombatMechanics.versions.materials;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Contains Materials that are different in {@literal > 1.13} and {@literal < 1.13}.
 */
public class MaterialRegistry {

    public static VersionedMaterial LAPIS_LAZULI = new DualVersionedMaterial(
            () -> new ItemStack(Material.matchMaterial("INK_SACK"), 1, (short) 4),
            () -> new ItemStack(Material.matchMaterial("LAPIS_LAZULI"))
    );

    public static VersionedMaterial ENCHANTED_GOLDEN_APPLE = new DualVersionedMaterial(
            () -> new ItemStack(Material.GOLDEN_APPLE, 1, (short) 1),
            () -> new ItemStack(Material.valueOf("ENCHANTED_GOLDEN_APPLE"))
    );
}
