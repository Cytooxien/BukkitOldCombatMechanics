package kernitus.plugin.OldCombatMechanics.utilities.damage;

import kernitus.plugin.OldCombatMechanics.OCMMain;
import kernitus.plugin.OldCombatMechanics.module.Module;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityDamageByEntityListener extends Module {

    private static EntityDamageByEntityListener INSTANCE;
    private boolean enabled;
    private final Map<UUID, Double> lastCorrectedDamage = new HashMap<>();

    public EntityDamageByEntityListener(OCMMain plugin) {
        super(plugin, "entity-damage-listener");
        INSTANCE = this;
    }

    public static EntityDamageByEntityListener getINSTANCE() {
        return INSTANCE;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (event.getEntity() instanceof LivingEntity livingEntity && lastCorrectedDamage.containsKey(livingEntity.getUniqueId())) {
            if ((float) livingEntity.getNoDamageTicks() > (float) livingEntity.getMaximumNoDamageTicks() / 2.0F) {
                final double damage = event.getDamage();
                // damage = minecraft 1.9 increased weapon damage.
                // damage <= lastCorrectedDamage, if that is the case the entity is getting damage it should not get
                if (damage <= lastCorrectedDamage.get(livingEntity.getUniqueId())) {
                    event.setCancelled(true);
                }
            } else {
                lastCorrectedDamage.remove(livingEntity.getUniqueId());
            }
        }

        OCMEntityDamageByEntityEvent e = new OCMEntityDamageByEntityEvent
                (damager, event.getEntity(), event.getCause(), event.getDamage());

        plugin.getServer().getPluginManager().callEvent(e);

        if (e.isCancelled()) return;

        //Re-calculate modified damage and set it back to original event
        // Damage order: base + potion effects + critical hit + enchantments + armour effects
        double newDamage = e.getBaseDamage();

        debug("Base: " + e.getBaseDamage(), damager);

        //Weakness potion
        double weaknessModifier = e.getWeaknessModifier();
        if (e.isWeaknessModifierMultiplier()) newDamage *= weaknessModifier;
        else newDamage += weaknessModifier;

        debug("Weak: " + e.getWeaknessModifier(), damager);

        //Strength potion
        debug("Strength level: " + e.getStrengthLevel(), damager);
        double strengthModifier = e.getStrengthModifier() * e.getStrengthLevel();
        if (!e.isStrengthModifierMultiplier()) newDamage += strengthModifier;
        else if (e.isStrengthModifierAddend()) newDamage *= ++strengthModifier;
        else newDamage *= strengthModifier;

        debug("Strength: " + strengthModifier, damager);

        // Critical hit: 1.9 is *1.5, 1.8 is *rand(0%,50%) + 1
        // Bukkit 1.8_r3 code:     i += this.random.nextInt(i / 2 + 2);
        if (e.was1_8Crit() && !e.wasSprinting()) {
            newDamage *= e.getCriticalMultiplier();
            if (e.RoundCritDamage()) newDamage = (int) newDamage;
            newDamage += e.getCriticalAddend();
            debug("Crit * " + e.getCriticalMultiplier() + " + " + e.getCriticalAddend(), damager);
        }

        //Enchantments
        newDamage += e.getMobEnchantmentsDamage() + e.getSharpnessDamage();

        debug("Mob " + e.getMobEnchantmentsDamage() + " Sharp: " + e.getSharpnessDamage(), damager);

        if (newDamage < 0) {
            debug("Damage was " + newDamage + " setting to 0", damager);
            newDamage = 0;
        }

        debug("New Damage: " + newDamage, damager);

        event.setDamage(newDamage);
    }

    /**
     * Set entity's last damage 1 tick after event. For some reason this is not updated to the final damage properly.
     * (Maybe a Spigot bug?) Hopefully other plugins vibe with this. Otherwise can store this just for OCM.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void afterEntityDamage(EntityDamageByEntityEvent e) {
        final Entity damagee = e.getEntity();
        if (damagee instanceof LivingEntity) {
            final double damage = e.getFinalDamage();
            lastCorrectedDamage.put(damagee.getUniqueId(), e.getDamage()); // we need to save the last damage because after the event it called minecraft ignores the new damage and overwrites the lastDamage with the regular calculated damage
            new BukkitRunnable() {
                @Override
                public void run() {
                    ((LivingEntity) damagee).setLastDamage(damage);
                    debug("Set last damage to " + damage, damagee);
                }
            }.runTaskLater(plugin, 1);

        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        lastCorrectedDamage.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastCorrectedDamage.remove(event.getPlayer().getUniqueId());
    }
}
