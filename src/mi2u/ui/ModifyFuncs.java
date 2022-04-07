package mi2u.ui;

import arc.Core;
import arc.func.Func;
import arc.math.Mathf;
import arc.util.Strings;
import mindustry.content.Liquids;
import mindustry.core.UI;
import mindustry.gen.Building;
import mindustry.graphics.*;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.consumers.ConsumeLiquid;
import mindustry.world.consumers.ConsumePower;
import mindustry.world.consumers.ConsumeType;

import static mindustry.Vars.*;

public class ModifyFuncs{

    public static void modifyVanillaBlockBars(){
        content.blocks().each(block -> {
            addBarToBlock(block, "health", e -> new Bar(() -> Core.bundle.format("stat.health") + ":" + Strings.autoFixed(e.health(), 3) + "(" + Strings.autoFixed(e.health * 100 / e.maxHealth, 2) + "%)", () -> Pal.health, e::healthf));
            if(block.hasLiquids){
                Func<Building, Liquid> current;
                if(block.consumes.has(ConsumeType.liquid) && block.consumes.get(ConsumeType.liquid) instanceof ConsumeLiquid){
                    Liquid liquid = block.consumes.<ConsumeLiquid>get(ConsumeType.liquid).liquid;
                    current = entity -> liquid;
                }else{
                    current = entity -> entity.liquids == null ? Liquids.water : entity.liquids.current();
                }
                addBarToBlock(block, "liquid", entity -> new Bar(() -> entity.liquids.get(current.get(entity)) <= 0.001f ? Core.bundle.get("bar.liquid") : (current.get(entity).localizedName + ":" + Strings.autoFixed(entity.liquids.get(current.get(entity)),2) + "/" + block.liquidCapacity),
                        () -> current.get(entity).barColor(), () -> entity == null || entity.liquids == null ? 0f : entity.liquids.get(current.get(entity)) / block.liquidCapacity));
            }

            if(block.hasPower && block.consumes.hasPower()){
                ConsumePower cons = block.consumes.getPower();
                boolean buffered = cons.buffered;
                float capacity = cons.capacity;

                addBarToBlock(block, "power", entity -> new Bar(() -> buffered ? Core.bundle.format("bar.poweramount", Float.isNaN(entity.power.status * capacity) ? "<ERROR>" : UI.formatAmount((int)(entity.power.status * capacity))) :
                        Core.bundle.get("bar.power") + ":" + Strings.autoFixed(-entity.power.status * cons.usage * 60f * (entity.cons().valid()?1:0),2), () -> Pal.powerBar, () -> Mathf.zero(cons.requestedPower(entity)) && entity.power.graph.getPowerProduced() + entity.power.graph.getBatteryStored() > 0f ? 1f : entity.power.status));
            }

            if(block instanceof Turret) addBarToBlock(block, "logicTimer", (Turret.TurretBuild entity) -> new Bar(() -> "Logic Control: " + Strings.autoFixed(entity.logicControlTime, 1), () -> Pal.logicControl, () -> entity.logicControlTime / Turret.logicControlCooldown));

        });
    }

    public static <T extends Building> boolean addBarToBlock(Block block, String name, Func<T, Bar> sup){
        block.bars.add(name, sup);
        return true;
    }
}
