package mi2u.ai;

import arc.*;
import arc.graphics.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.MI2UTmp;
import mi2u.input.*;
import mi2u.io.*;
import mi2u.ui.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;
import static mi2u.MI2UVars.*;

public class FullAI extends AIController{
    public Seq<Mode> modes = new Seq<>();
    protected Interval timer = new Interval(8);

    public FullAI(){
        super();
        modes.add(new BaseMineMode());
        modes.add(new AutoBuildMode());
        modes.add(new SelfRepairMode());
        modes.add(new AutoTargetMode());
    }

    @Override
    public void updateUnit(){
        if(!(control.input instanceof InputOverwrite) && MI2USettings.getBool("inputReplace", true)) {
            if(mobile){
                MobileInputExt.mobileExt.replaceInput();
            }else{
                DesktopInputExt.desktopExt.replaceInput();
            }
        }
        if(control.input instanceof InputOverwrite inp){
            inp.clear();
            modes.each(Mode::act);
        }
    }

    /**unit actions can be covered by the lasted related mode. Executed after each mode acted.*/
    public void moveAction(Position target, float radius, boolean checkWithin){
        if(control.input instanceof InputOverwrite inp) inp.approach(target, radius, checkWithin);
    }

    public void boostAction(boolean boost){
        if(control.input instanceof InputOverwrite inp) inp.boost(boost);
    }

    public void shootAction(Vec2 point, Boolean shoot){
        if(control.input instanceof InputOverwrite inp) inp.shoot(point, shoot, true);
    }

    public class Mode{
        public boolean enable = false;
        public String btext;
        public Mode(){
            btext = Iconc.units + "";
        }
        /** override it. enable should be checked first */
        public void act(){}

        public void buildConfig(Table table){
            table.table(t -> {
                t.setBackground(Mindow2.gray2);
                t.add(btext).color(Color.sky).left();
                t.add().growX();
            }).growX().minHeight(18f).padTop(8f);
            table.row();
        }
    }

    public class BaseMineMode extends Mode{
        public Seq<Item> list = new Seq<>();
        boolean mining;
        Item targetItem;
        Tile ore;
        
        public BaseMineMode(){
            btext = Iconc.unitMono + "";
            list.add(Items.copper, Items.lead);
        }

        @Override
        public void act(){
            if(!enable) return;
            Building core = unit.closestCore();
            boostAction(true);
            if(!(unit.canMine()) || core == null) return;
            if(unit.mineTile != null && !unit.mineTile.within(unit, unit.type.miningRange)){
                unit.mineTile(null);
            }
            if(mining){
                if(timer.get(0, 60 * 4) || targetItem == null){
                    targetItem = list.min(i -> indexer.hasOre(i) && unit.canMine(i), i -> core.items.get(i));
                }
                //core full of the target item, do nothing
                if(targetItem != null && core.acceptStack(targetItem, 1, unit) == 0){
                    unit.clearItem();
                    unit.mineTile = null;
                    return;
                }
                //if inventory is full, drop it off.
                if(unit.stack.amount >= unit.type.itemCapacity || (targetItem != null && !unit.acceptsItem(targetItem))){
                    mining = false;
                }else{
                    if(timer.get(1, 60) && targetItem != null){
                        ore = indexer.findClosestOre(unit, targetItem);
                    }
                    if(ore != null){
                        moveAction(ore, 50f, false);
                        if(ore.block() == Blocks.air && unit.within(ore, unit.type.miningRange)){
                            unit.mineTile = ore;
                        }
                        if(ore.block() != Blocks.air){
                            mining = false;
                        }
                    }
                }
            }else{
                unit.mineTile = null;
                if(unit.stack.amount == 0){
                    mining = true;
                    return;
                }
                if(unit.within(core, itemTransferRange / 1.5f) && timer.get(2, 120f)){
                    if(core.acceptStack(unit.stack.item, unit.stack.amount, unit) > 0){
                        Call.transferInventory(unit.getPlayer(), core);
                    }
                    unit.clearItem();
                    mining = true;
                }
                moveAction(core, itemTransferRange / 2f, false);
            }
        }

        @Override
        public void buildConfig(Table table) {
            super.buildConfig(table);
            table.table(t -> {
                int i = 0;
                for(var item : content.items()){
                    if(!content.blocks().contains(b -> b.itemDrop == item)) continue;
                    t.button(b -> {
                        b.image(item.uiIcon).size(16f);
                        b.add(item.localizedName);
                        b.margin(4f);
                        }, textbtoggle, () -> {
                        if(list.contains(item)){
                            list.remove(item);
                        }else {
                            list.add(item);
                        }
                    }).fill().update(b -> b.setChecked(list.contains(item)));
                    i++;
                    if(i >= 3){
                        i = 0;
                        t.row();
                    }
                }
            });
        }
    }

    public class AutoBuildMode extends Mode{
        public boolean rebuild = false, follow = true;
        private BuildPlan cobuildplan;
        public AutoBuildMode(){
            btext = Iconc.unitPoly + "";
        }
        @Override
        public void act(){
            if(!enable) return;
            if(!control.input.isBuilding) return;
            if(!unit.canBuild()) return;
            //help others building
            if(follow && timer.get(3, 20f) && unit.plans().isEmpty()){
                cobuildplan = null;

                for(var player : Groups.player){
                    var u = player.unit();
                    if(u == null || u.team != unit.team) continue;

                    if(u.canBuild() && u != unit && u.activelyBuilding() && u.buildPlan() != null){
                        BuildPlan plan = u.buildPlan();
                        Building build = world.build(plan.x, plan.y);
                        if(build instanceof ConstructBlock.ConstructBuild cons){
                            float dist = Math.min(cons.dst(unit) - buildingRange, 0);

                            //make sure you can reach the request in time
                            if(dist / unit.speed() < cons.buildCost * 0.9f && (cobuildplan == null || MI2UTmp.v1.set(u.buildPlan()).dst(unit) < MI2UTmp.v2.set(cobuildplan).dst(unit))){
                                cobuildplan = u.buildPlan();
                            }
                        }
                    }
                }

                if(cobuildplan != null) unit.plans.addFirst(cobuildplan);

            }else if(rebuild && timer.get(4, 30f) && unit.plans().isEmpty() && !unit.team.data().blocks.isEmpty()){
                //rebuild
                var block = unit.team.data().blocks.first();
                if(world.tile(block.x, block.y) != null && world.tile(block.x, block.y).block().id == block.block){
                    state.teams.get(player.team()).blocks.remove(block);
                }else{
                    unit.addBuild(new BuildPlan(block.x, block.y, block.rotation, content.block(block.block), block.config));
                }

            }else if(timer.get(5, 60f) && unit.buildPlan() != null){
                //cancel co-op plan that someone has conflicting idea or no player is building anymore
                boolean cobuilding = false;
                for(var player : Groups.player){
                    var u = player.unit();
                    if(u == null || u.team != unit.team) continue;
                    if(u.canBuild() && u != unit && u.activelyBuilding() && u.buildPlan() != null){
                        BuildPlan plan = u.buildPlan();
                        if(cobuildplan != null && plan.samePos(cobuildplan) && plan.block == cobuildplan.block){
                            if(plan.breaking != cobuildplan.breaking){
                                cobuilding = false;
                                break;
                            }else{
                                cobuilding = true;
                            }
                        }
                    }
                }
                //cancel co-build plan that no other unit is building.
                if(!cobuilding && cobuildplan != null){
                    unit.plans().remove(bp -> bp.x == cobuildplan.x && bp.y == cobuildplan.y && bp.block == cobuildplan.block && bp.breaking == cobuildplan.breaking);
                    cobuildplan = null;
                }
            }
            if(unit.plans().isEmpty()) return;
            boostAction(true);
            moveAction(unit.plans().first(), buildingRange / 1.4f, true);
        }

        @Override
        public void buildConfig(Table table) {
            super.buildConfig(table);
            table.table(t -> {
                t.button("@ai.config.autorebuild", textbtoggle, () -> rebuild = !rebuild).update(b -> b.setChecked(rebuild)).with(funcSetTextb);
                t.button("@ai.config.follow", textbtoggle, () -> follow = !follow).update(b -> b.setChecked(follow)).with(funcSetTextb);
            }).growX();

        }
    }

    public class SelfRepairMode extends Mode{
        public SelfRepairMode(){
            btext = Iconc.blockRepairPoint + "";
        }
        @Override
        public void act(){
            if(!enable) return;
            if(unit.dead || !unit.isValid()) return;
            if(unit.health > unit.maxHealth * 0.6f) return;
            Tile tile = Geometry.findClosest(unit.x, unit.y, indexer.getAllied(unit.team, BlockFlag.repair));
            if(tile == null) return;
            boostAction(true);
            moveAction(tile, 20f, false);
        }
    }

    public class AutoTargetMode extends Mode{
        public boolean attack = true, heal = true;
        public AutoTargetMode(){
            btext = "AT";
        }
        @Override
        public void act(){
            if(!enable) return;
            if(Core.input.keyDown(Binding.select)) return;

            if(timer.get(6, 30f)){
                target = null;
                float range = unit.hasWeapons() ? unit.range() : 0f;
                if(attack) target = Units.closestTarget(unit.team, unit.x, unit.y, range, u -> u.checkTarget(unit.type.targetAir, unit.type.targetGround), u -> unit.type.targetGround);
                if(heal && unit.type.canHeal && target == null){
                    target = Geometry.findClosest(unit.x, unit.y, indexer.getDamaged(unit.team));
                    if(target != null && !unit.within(target, range)){
                        target = null;
                    }
                }
            }

            if(target != null){
                Vec2 intercept = Predict.intercept(unit, target, unit.hasWeapons() ? unit.type.weapons.first().bullet.speed : 0f);
                shootAction(intercept, true);
            }else{
                shootAction(MI2UTmp.v1.set(player.mouseX, player.mouseY), false);
            }
        }

        @Override
        public void buildConfig(Table table) {
            super.buildConfig(table);
            table.table(t -> {
                t.button("@ai.config.attack", textbtoggle, () -> attack = !attack).update(b -> b.setChecked(attack)).with(funcSetTextb);
                t.button("@ai.config.heal", textbtoggle, () -> heal = !heal).update(b -> b.setChecked(heal)).with(funcSetTextb);
            }).growX();
        }
    }
}
