package kernitus.plugin.OldCombatMechanics.module;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;

import kernitus.plugin.OldCombatMechanics.OCMMain;

@SuppressWarnings("deprecation")
public class ModuleShieldDamageReduction extends Module {

	private String blockingDamageReduction;

	public ModuleShieldDamageReduction(OCMMain plugin) {
		super(plugin, "shield-damage-reduction");

	}

	@Override
	public void reload(){
		blockingDamageReduction = module()
				.getString("blockingDamageReduction", "50%")
				.replaceAll(" ", "");
	}


	@EventHandler(priority = EventPriority.HIGHEST)
	public void onHit(EntityDamageByEntityEvent e){
		Entity ent = e.getEntity();

		if(!(ent instanceof Player)) return;

		Player p = (Player) ent;

		//if(isBlocking(p.getUniqueId())){
		//Only reduce damage if they were hit head on, i.e. the shield blocked some of the damage
		if(e.getDamage(DamageModifier.BLOCKING) >= 0) return;
		
		
		//Instead of reducing damage to 33% apply config reduction

		double damageReduction = e.getDamage(); //Reducing by this would mean blocking all damage

		if(blockingDamageReduction.matches("\\d{1,3}%")){
			//Reduce damage by percentage
			int percentage = Integer.parseInt(blockingDamageReduction.replace("%", ""));
			damageReduction = (damageReduction - 1) * percentage / 100;
		} else if(blockingDamageReduction.matches("\\d+")){
			//Reduce by specified amount of half-hearts
			damageReduction = Integer.parseInt(blockingDamageReduction);
		} else damageReduction = 0;

		if(damageReduction < 0) damageReduction = 0;

		//Also make sure reducing the damage doesn't result in negative damage
		e.setDamage(DamageModifier.BLOCKING, 0);

		if(e.getFinalDamage() >= damageReduction)
			e.setDamage(DamageModifier.BLOCKING, damageReduction * -1);

		debug("Damage reduced by: " + e.getDamage(DamageModifier.BLOCKING), p);
	}
}