package mi2u.ai;

import arc.*;
import arc.graphics.*;
import arc.math.*;
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
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.type.weapons.RepairBeamWeapon;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;
import static mi2u.MI2UVars.*;

public class FullAI extends AIController{
    public Seq<Mode> modes = new Seq<>();
    protected Interval timer = new Interval(9);

    public FullAI(){
        super();
        modes.add(new CenterFollowMode());
        modes.add(new BaseMineMode());
        modes.add(new AutoBuildMode());
        modes.add(new SelfRepairMode());
        modes.add(new AutoTargetMode());

        Events.run(EventType.Trigger.update, () -> {
            fullAI.unit(player.unit());
            fullAI.updateUnit();
        });
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
            if(unit.mineTile != null && !unit.mineTile.within(unit, unit.type.mineRange)){
                unit.mineTile = null;
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
                        if(ore.block() == Blocks.air && unit.within(ore, unit.type.mineRange)){
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

                if(unit.within(core, itemTransferRange / 1.5f)){
                    if(!timer.check(2, 120f)) shootAction(MI2UTmp.v1.set(core).lerpDelta(Mathf.range(-12f, 12f), Mathf.range(-8f, 8f), 0.1f), true);
                    if(timer.get(2, 120f)){
                        control.input.droppingItem = true;
                        control.input.tryDropItems(core, unit.aimX, unit.aimY);
                    }
                    mining = true;
                }
                moveAction(core, itemTransferRange / 2f, false);
            }
        }

        @Override
        public void buildConfig(Table table) {
            super.buildConfig(table);
            table.pane(p -> {
                int i = 0;
                for(var item : content.items()){
                    if(!content.blocks().contains(b -> b.itemDrop == item)) continue;
                    p.button(b -> {
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
                        p.row();
                    }
                }
            }).maxHeight(300f).with(p -> p.setFadeScrollBars(false));
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
            //help others building, catching the closest plan to co-op.
            if(follow && timer.get(3, 15f) && (unit.plans().isEmpty() || (cobuildplan != null && unit.buildPlan().samePos(cobuildplan) && unit.buildPlan().block == cobuildplan.block && unit.buildPlan().breaking == cobuildplan.breaking))){
                Point2 last = cobuildplan == null ? null : MI2UTmp.p1.set(cobuildplan.x, cobuildplan.y);
                cobuildplan = null;

                for(var player : Groups.player){
                    if(player.unit() == null || player.team() != unit.team) continue;

                    var u = player.unit();
                    if(u.canBuild() && u != unit && u.activelyBuilding() && u.buildPlan() != null){
                        BuildPlan plan = u.buildPlan();
                        if(world.build(plan.x, plan.y) instanceof ConstructBlock.ConstructBuild cons){
                            float dist = Math.min(cons.dst(unit) - buildingRange, 0);

                            //make sure you can reach the request in time
                            if(dist / unit.speed() < cons.buildCost * 0.9f && (cobuildplan == null || MI2UTmp.v1.set(u.buildPlan()).dst(unit) < MI2UTmp.v2.set(cobuildplan).dst(unit))){
                                cobuildplan = plan;
                            }
                        }
                    }
                }

                if(cobuildplan != null){
                    if(last != null) unit.plans.remove(bp -> bp.x == last.x && bp.y == last.y);
                    unit.plans.addFirst(cobuildplan);
                }

            }

            if(rebuild && timer.get(4, 30f) && unit.plans().isEmpty() && !unit.team.data().plans.isEmpty()){
                //rebuild
                var block = unit.team.data().plans.first();
                if(world.tile(block.x, block.y) != null && world.tile(block.x, block.y).block().id == block.block){
                    state.teams.get(player.team()).plans.remove(block);
                }else{
                    unit.addBuild(new BuildPlan(block.x, block.y, block.rotation, content.block(block.block), block.config));
                }

            }

            if(timer.get(5, 60f) && unit.buildPlan() != null && cobuildplan != null){
                //cancel co-op plan that someone has conflicting idea or no player is building anymore
                boolean cobuilding = false;
                for(var player : Groups.player){
                    if(player.unit() == null || player.team() != unit.team) continue;
                    var u = player.unit();
                    if(u.canBuild() && u != unit && u.activelyBuilding() && u.buildPlan() != null){
                        BuildPlan plan = u.buildPlan();
                        if(plan.samePos(cobuildplan) && plan.block == cobuildplan.block){
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
                if(!cobuilding){
                    unit.plans.remove(bp -> bp.samePos(cobuildplan) && bp.breaking == cobuildplan.breaking);
                    cobuildplan = null;
                }
            }

            if(unit.plans().isEmpty()) return;
            boostAction(true);
            float mindst = Float.MAX_VALUE, tmp;
            BuildPlan min = unit.plans.first();
            if(unit.plans.size < 128){
                for(var bp : unit.plans){
                    tmp = MI2UTmp.v1.set(bp).dst(unit);
                    if(tmp < mindst){
                        min = bp;
                        mindst = tmp;
                    }
                    if(tmp < buildingRange) break;
                }
            }
            moveAction(min, buildingRange / 1.4f, true);
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
            Building tile = Geometry.findClosest(unit.x, unit.y, indexer.getFlagged(unit.team, BlockFlag.repair));
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
                if(heal && (unit.type.canHeal || unit.type.weapons.contains(w -> w instanceof RepairBeamWeapon)) && target == null){
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

    public class CenterFollowMode extends Mode{
        public Vec2 unitPos = new Vec2();
        Item targetItem;
        boolean onetimePick = true;
        public CenterFollowMode(){
            btext = Iconc.move + "";
            enable = mobile;
        }
        @Override
        public void act(){
            if(unit.dead || !unit.isValid()) return;

            if(targetItem != null && (unit.stack.item != targetItem || unit.stack.amount < 10)){
                Building core = unit.closestCore();
                boostAction(true);
                moveAction(core, itemTransferRange / 2f, false);

                if(unit.within(core, itemTransferRange / 1.5f)){
                    if(timer.get(8, 120f)){
                        if(unit.stack.item != null && unit.stack.item != targetItem && unit.stack.amount > 0){
                            control.input.droppingItem = true;
                            control.input.tryDropItems(core, unit.aimX, unit.aimY);
                        }else{
                            Call.requestItem(player, core, targetItem, unit.itemCapacity());
                        }
                    }
                }
                if(onetimePick && unit.stack.item == targetItem && unit.stack.amount > 0) targetItem = null;

            }else if(mobile){
                if(!enable){
                    boostAction(true);
                    moveAction(unit, 0.01f, false);
                }

            }else if(enable){
                boostAction(true);
                moveAction(Core.camera.position, 15f, false);
            }
        }

        @Override
        public void buildConfig(Table table) {
            super.buildConfig(table);
            table.pane(p -> {
                int i = 0;
                for(var item : content.items()){
                    p.button(b -> {
                        b.image(item.uiIcon).size(24f).update(img -> {
                            img.setColor(b.isDisabled() ? Color.gray : Color.white);
                        });
                        b.margin(4f);
                    }, textbtoggle, () -> {
                        targetItem = targetItem == item ? null : item;
                    }).fill().checked(b -> targetItem == item).disabled(b -> {
                        var core = player.team().core();
                        return !state.isGame() || core == null || core.items == null || !core.items.has(item);
                    });
                    i++;
                    if(i >= 5){
                        i = 0;
                        p.row();
                    }
                }
            }).maxHeight(300f).with(p -> p.setFadeScrollBars(false));
            table.row();
            table.button("@ai.config.oneTime", textbtoggle, () -> onetimePick = !onetimePick).growX().update(b -> b.setChecked(onetimePick)).with(funcSetTextb);
        }
    }
}
