package mi2u.ai;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.*;
import mi2u.game.*;
import mi2u.input.*;
import mi2u.io.*;
import mi2u.ui.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.logic.*;
import mindustry.logic.LExecutor.*;
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
        modes.add(new LogicMode());

        Events.run(EventType.Trigger.update, () -> {
            if(state.isGame() && state.isPlaying()){
                fullAI.unit(player.unit());
                fullAI.updateUnit();
            }
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
            modes.each(mode -> {
                if(mode.enable) mode.act();
            });
        }
    }

    /**unit actions can be covered by the lasted related mode. Executed after each mode acted.*/
    public void moveAction(Position target, float radius, boolean checkWithin){
        if(control.input instanceof InputOverwrite inp) inp.approach(target, radius, checkWithin);
    }

    public void moveAction(float x, float y, float radius, boolean checkWithin){
        if(control.input instanceof InputOverwrite inp) inp.approach(MI2UTmp.v3.set(x, y), radius, checkWithin);
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
        public Drawable bimg;
        public Mode(){
            btext = Iconc.units + "";
        }
        /** override it. enable auto checked. */
        public void act(){}

        public void buildConfig(Table table){
            table.table(t -> {
                t.setBackground(Mindow2.gray2);
                t.button(b -> {
                    b.image().grow().update(img -> img.setColor(enable ? Color.acid : Color.red));
                }, textb, () -> {
                    enable = !enable;
                }).size(16f);
                if(bimg != null){
                    t.image(bimg).size(18f).scaling(Scaling.fit);
                }else{
                    t.add(btext).color(Color.sky).left();
                }
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
            Building core = unit.closestCore();
            boostAction(true);
            if(!(unit.canMine()) || !unit.type.mineFloor || core == null) return;
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
                        if(unit.type.mineWalls){
                            //TODO wall ore
                        }else{
                            ore = indexer.findClosestOre(unit, targetItem);
                        }
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
                        b.image(item.uiIcon).size(24f);
                        b.margin(4f);
                        }, textbtoggle, () -> {
                        if(list.contains(item)){
                            list.remove(item);
                        }else {
                            list.add(item);
                        }
                    }).fill().update(b -> b.setChecked(list.contains(item)));
                    i++;
                    if(i >= 7){
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
                            if(dist / unit.speed() < cons.buildCost * 0.9f && (cobuildplan == null || MI2UTmp.v1.set(plan).dst(unit) < MI2UTmp.v2.set(cobuildplan).dst(unit))){
                                boolean itemreq = true;
                                for(var item : plan.block.requirements){
                                    if(player.team().core() != null && !player.team().core().items.has(item.item)){
                                        itemreq = false;
                                        break;
                                    }
                                }
                                if(itemreq) cobuildplan = plan;
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
        public float hold = 0.6f;
        public SelfRepairMode(){
            btext = Iconc.blockRepairPoint + "";
        }
        @Override
        public void act(){
            if(unit.dead || !unit.isValid()) return;
            if(unit.health > unit.maxHealth * hold) return;
            Building tile = Geometry.findClosest(unit.x, unit.y, indexer.getFlagged(unit.team, BlockFlag.repair));
            if(tile == null) return;
            boostAction(true);
            moveAction(tile, 20f, false);
        }

        @Override
        public void buildConfig(Table table){
            super.buildConfig(table);
            table.slider(0f, 1f, 0.01f, 0.6f, f -> hold = f).get().setValue(hold);
        }
    }

    public class AutoTargetMode extends Mode{
        public boolean attack = true, heal = true;
        public AutoTargetMode(){
            btext = "AT";
            bimg = Core.atlas.drawable("mi2-utilities-java-ui-shoot");
        }
        @Override
        public void act(){
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
        Item targetItem;
        boolean onetimePick = true;
        public CenterFollowMode(){
            btext = Iconc.move + "";
            enable = mobile;
            bimg = Core.atlas.drawable("mi2-utilities-java-ui-centermove");
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
                    if(i >= 7){
                        i = 0;
                        p.row();
                    }
                }
            }).maxHeight(300f).with(p -> p.setFadeScrollBars(false));
            table.row();
            table.button("@ai.config.oneTime", textbtoggle, () -> onetimePick = !onetimePick).growX().update(b -> b.setChecked(onetimePick)).with(funcSetTextb);
        }
    }

    public class LogicMode extends Mode{
        public static final Seq<Class<? extends LInstruction>> bannedInstructions = new Seq<>();
        public LExecutor exec = new LExecutor();
        public String code;
        public LogicAI ai = new LogicAI();
        public int instructionsPerTick = 100;
        MI2Utils.IntervalMillis timer = new MI2Utils.IntervalMillis();
        MI2Utils.IntervalMillis actionTimer = new MI2Utils.IntervalMillis(2);
        public boolean itemTrans, payloadTrans;
        public static StringBuffer log = new StringBuffer();
        Queue<BuildPlan> plans = new Queue<>();

        //public int lastPathId = 0;
        //public float lastMoveX, lastMoveY;

        public LogicMode(){
            super();
            bannedInstructions.clear();
            bannedInstructions.addAll(ControlI.class, WriteI.class, StopI.class, SetBlockI.class, SpawnUnitI.class, ApplyEffectI.class, SetRuleI.class, SetRateI.class, ExplosionI.class, SetFlagI.class, SpawnWaveI.class, SetPropI.class);
            btext = Iconc.blockWorldProcessor + "";
            Events.on(MI2UEvents.FinishSettingInitEvent.class, e -> {
                code = MI2USettings.getStr("ai.logic.code.0");
                readCode(code);
            });
        }

        @Override
        public void act(){
            var ctrl = unit.controller();
            unit.controller(ai);

            updatePlayerActionTimer();
            if(timer.get(200)){
                ai.targetTimer = 0f;
                ai.controlTimer = LogicAI.logicControlTimeout;
                ai.updateMovement();
            }

            plans.clear();
            if(unit.plans != null) unit.plans.each(bp -> plans.add(bp));
            for(int i = 0; i < Mathf.clamp(instructionsPerTick, 1, 2000); i++){
                if(exec.instructions.length == 0) break;
                exec.setconst(LExecutor.varUnit, unit);
                if(tryRunOverwrite(exec.instructions[Mathf.mod((int)(exec.counter.numval), exec.instructions.length)])){
                    exec.counter.numval++;
                    continue;
                }
                if(!isLocalSandbox() && bannedInstructions.contains(exec.instructions[Mathf.mod((int)(exec.counter.numval), exec.instructions.length)].getClass())){
                    exec.counter.numval++;
                    continue;
                }
                exec.runOnce();
            }
            if(unit.plans != null && unit.plans.isEmpty()) plans.each(bp -> unit.plans.add(bp));

            unit.controller(ctrl);
            fullAI.unit(unit);

            if(!actionTimer.check(0, (int)LogicAI.logicControlTimeout / 60 * 1000)){
                boostAction(ai.boost);
                if(ai.control != LUnitControl.pathfind || unit.isFlying()){
                    moveAction(ai.moveX, ai.moveY, Math.max(ai.moveRad, 1f), false);
                }/*else{
                if(!Mathf.equal(ai.moveX, lastMoveX, 0.1f) || !Mathf.equal(ai.moveY, lastMoveY, 0.1f)){
                    lastPathId ++;
                    lastMoveX = ai.moveX;
                    lastMoveY = ai.moveY;
                }
                if(Vars.controlPath.getPathPosition(unit, lastPathId, Tmp.v2.set(ai.moveX, ai.moveY), Tmp.v1, null)){
                    moveTo(Tmp.v1, 1f, Tmp.v2.epsilonEquals(Tmp.v1, 4.1f) ? 30f : 0f);
                }
                moveAction(Tmp.v1, Math.max(ai.moveRad, 1f), false);//tmp.v1 is set in ai.updateMovement()
            }*/
            }

            if(!actionTimer.check(1, (int)LogicAI.logicControlTimeout / 60 * 1000)){
                var tgt = ai.target(0, 0, 0, false, false);
                if(tgt != null) shootAction(MI2UTmp.v3.set(tgt.getX(), tgt.getY()), ai.shoot);
            }
        }

        @Override
        public void buildConfig(Table table){
            super.buildConfig(table);
            table.table(t -> {
                t.name = "cfg";
                t.button(Iconc.pencil + "" + Iconc.blockWorldProcessor, textb, () -> {
                    ui.logic.show(code, exec, true, str -> {
                        readCode(str);
                    });
                }).grow().minWidth(40f);
                t.add("ipt=");
                t.field(String.valueOf(instructionsPerTick), TextField.TextFieldFilter.digitsOnly, str -> instructionsPerTick = Strings.parseInt(str)).growX();
                t.button(Iconc.info + "", textb, () -> {
                    ui.showText("", Core.bundle.get("fullAI.help"), Align.left);
                }).size(32f);
            }).grow();
            table.row();
            table.add("Print Log").left().color(Color.royal);
            table.row();
            table.pane(t -> {
                t.name = "log";
                t.image().color(Color.royal).growY().width(2f);
                t.labelWrap(() -> log).grow();
            }).grow().maxHeight(200f);

        }

        public void readCode(String str){
            code = str;
            MI2USettings.putStr("ai.logic.code.0", code);
            LAssembler asm = LAssembler.assemble(str, true);
            asm.putConst("@mapw", world.width());
            asm.putConst("@maph", world.height());
            asm.putConst("@links", exec.links.length);
            asm.putConst("@ipt", instructionsPerTick);
            exec.load(asm);
            exec.privileged = true;
        }

        public void updatePlayerActionTimer(){
            itemTrans = true;
            payloadTrans = true;
        }

        public boolean isLocalSandbox(){
            return !net.client() || state.rules.mode() == Gamemode.sandbox;
        }

        public boolean tryRunOverwrite(LInstruction inst){
            if(inst instanceof ControlI li){
                if(!player.dead() && exec.obj(li.target) instanceof Building b && (isLocalSandbox() || b.team == exec.team)){
                    if(li.type == LAccess.config){
                        b.configured(player.unit(), exec.obj(li.p1));
                    }
                }
                return true;
            }else if(inst instanceof PrintFlushI){
                log.setLength(0);
                log.append(exec.textBuffer, 0, exec.textBuffer.length());
                exec.textBuffer.setLength(0);
            }else if(inst instanceof UnitControlI li){
                switch(li.type){
                    case target, targetp -> {
                        actionTimer.get(1, 1);
                        return false;
                    }
                    case move, pathfind, boost -> {
                        actionTimer.get(0, 1);
                        return false;
                    }
                    case idle -> {
                        actionTimer.reset(0, 100000);
                        return false;
                    }
                    case stop -> {
                        actionTimer.reset(1, 100000);
                        return false;
                    }
                    case itemTake -> {
                        if(!(exec.obj(li.p2) instanceof Item item)) return false;
                        if(!itemTrans || player.unit() == null || !player.unit().acceptsItem(item)) return false;
                        Building build = exec.building(li.p1);
                        if(build != null && build.team == unit.team && build.isValid() && build.items != null && unit.within(build, itemTransferRange + build.block.size * tilesize/2f)){
                            Call.requestItem(player, build, item, exec.numi(li.p3));
                            itemTrans = false;
                        }
                        return true;
                    }
                    case itemDrop -> {
                        if(!itemTrans || player.unit() == null || player.unit().stack.amount == 0) return false;
                        Building build = exec.building(li.p1);
                        if(build != null && unit.within(build, itemTransferRange + build.block.size * tilesize/2f)){
                            control.input.droppingItem = true;
                            control.input.tryDropItems(build, 0f, 0f);
                            control.input.droppingItem = false;
                            itemTrans = false;
                        }else if(exec.obj(li.p1) == Blocks.air){
                            control.input.tryDropItems(null, 0f, 0f);
                            itemTrans = false;
                        }
                        return true;
                    }
                    case payTake -> {
                        if(!payloadTrans) return false;
                        control.input.tryPickupPayload();
                        payloadTrans = false;
                        return true;
                    }
                    case payDrop -> {
                        if(!payloadTrans) return false;
                        control.input.tryDropPayload();
                        payloadTrans = false;
                        return true;
                    }
                    case payEnter -> {
                        if(!payloadTrans) return false;
                        Building build = world.buildWorld(unit.x, unit.y);
                        if(build != null){
                            payloadTrans = false;
                            Call.buildingControlSelect(player, build);
                        }
                        return true;
                    }
                    case flag -> {
                        return true;
                    }
                }
            }

            return false;
        }
    }
}
