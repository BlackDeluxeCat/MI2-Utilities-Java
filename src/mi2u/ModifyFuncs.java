package mi2u;

import arc.Core;
import arc.Events;
import arc.func.Func;
import arc.func.Prov;
import arc.math.Mathf;
import arc.util.Log;
import arc.util.Strings;
import mi2u.io.MI2USettings;
import mi2u.map.filters.*;
import mindustry.content.Liquids;
import mindustry.core.UI;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.graphics.*;
import mindustry.maps.Maps;
import mindustry.maps.filters.GenerateFilter;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.consumers.ConsumeLiquid;
import mindustry.world.consumers.ConsumePower;
import mindustry.world.consumers.ConsumeType;

import java.util.Arrays;

import static mindustry.Vars.*;

/** modify vanilla game*/
public class ModifyFuncs{

    public static void modifyVanilla(){
        modifyVanillaBlockBars();
        Events.on(EventType.ContentInitEvent.class, e2 -> modifyVanillaBlockBars());
        addFilters();
    }

    public static void addFilters(){
        if(!MI2USettings.getBool("modifyFilters")) return;
        addFilter(AdvancedNoiseFilter::new);
        addFilter(AdvancedOreFilter::new);
        addFilter(GridFilter::new);
        addFilter(CopyPasteFilter::new);
    }

    public static void addFilter(Prov<GenerateFilter> filter){
        var newArr = Arrays.copyOf(Maps.allFilterTypes, Maps.allFilterTypes.length + 1);
        newArr[Maps.allFilterTypes.length] = filter;
        Maps.allFilterTypes = newArr;
        Log.info("Adding New Filters... Filters Size: " + newArr.length);
    }

    public static void modifyVanillaBlockBars(){
        if(!MI2USettings.getBool("modifyBlockBars")) return;
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
