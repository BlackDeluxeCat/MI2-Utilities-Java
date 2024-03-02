package mi2u.ai;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.*;
import mi2u.input.*;
import mi2u.struct.*;
import mi2u.ui.*;
import mi2u.ui.elements.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.logic.*;
import mindustry.logic.LExecutor.*;
import mindustry.type.*;
import mindustry.type.weapons.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.meta.*;

import static mi2u.MI2UVars.*;
import static mindustry.Vars.*;

public class FullAI extends AIController{
    public Seq<Mode> modes = new Seq<>();

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
        if(!(control.input instanceof InputOverwrite) && mi2ui.settings.getBool("inputReplace")) {
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

        public boolean configUIExpand = true;
        Interval timer = new Interval(10);

        public Mode(){
            btext = Iconc.units + "";
        }
        /** override it. enable auto checked. */
        public void act(){}

        public void buildConfig(Table table){}
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
            if(!(unit.canMine()) || core == null) return;
            if(unit.mineTile != null && !unit.mineTile.within(unit, unit.type.mineRange)){
                unit.mineTile = null;
            }
            if(mining){
                if(timer.get(0, 60 * 4) || targetItem == null){
                    targetItem = list.min(i -> unit.canMine(i), i -> core.items.get(i));
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
                    if(timer.get(1, 120) && targetItem != null){
                        if(unit.type.mineWalls){
                            if(ore != null && ore.wallDrop() != targetItem) ore = null;
                            //it will be a bit laggy
                            if(timer.get(6, 60 * 60)) WorldData.scanWorld();
                            var seq = content.blocks().select(b -> (b.solid || (b instanceof Floor f && f.wallOre)) && b.itemDrop == targetItem);
                            for(var block : seq){
                                if(WorldData.countBlock(block, null) > 0){
                                    WorldData.getSeq(block, null).each(pos -> {
                                        if(ore != null && unit.within(ore, unit.type.mineRange)) return;
                                        if((ore == null || MI2UTmp.v3.set(ore).sub(unit).len2() > MI2UTmp.v2.set(Point2.x(pos), Point2.y(pos)).scl(tilesize).sub(unit).len2()) && world.tile(pos).wallDrop()== targetItem) ore = world.tile(pos);
                                    });
                                }
                            }
                        }else{
                            ore = indexer.findClosestOre(unit, targetItem);
                        }
                    }
                    if(ore != null){
                        moveAction(ore, 50f, false);
                        if(unit.within(ore, unit.type.mineRange)){
                            unit.mineTile = ore;
                        }
                    }
                }
            }else{
                unit.mineTile = null;
                if(unit.stack.amount == 0){
                    mining = true;
                    return;
                }

                if(unit.within(core, itemTransferRange / 1.1f)){
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
            if(follow && timer.get(0, 15f) && (unit.plans().isEmpty() || (cobuildplan != null && unit.buildPlan().samePos(cobuildplan) && unit.buildPlan().block == cobuildplan.block && unit.buildPlan().breaking == cobuildplan.breaking))){
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

            if(rebuild && timer.get(1, 30f) && unit.plans().isEmpty() && !unit.team.data().plans.isEmpty()){
                //rebuild
                var block = unit.team.data().plans.first();
                if(world.tile(block.x, block.y) != null && world.tile(block.x, block.y).block().id == block.block){
                    state.teams.get(player.team()).plans.remove(block);
                }else{
                    unit.addBuild(new BuildPlan(block.x, block.y, block.rotation, content.block(block.block), block.config));
                }

            }

            if(timer.get(2, 60f) && unit.buildPlan() != null && cobuildplan != null){
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

            if(timer.get(0, 30f)){
                target = null;
                float range = unit.hasWeapons() ? unit.range() : unit instanceof BlockUnitUnit build ? build.tile() instanceof Turret.TurretBuild tb ? tb.range() : 0f : 0f;
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
                    if(timer.get(0, 120f)){
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
        public static LogicMode logicMode;
        public LogicModeCode code;
        public Seq<LogicModeCode> codes;

        public LExecutor exec = new LExecutor();
        public LogicAI ai = new LogicAI();
        public int instructionsPerTick = 100;
        MI2Utils.IntervalMillis timer = new MI2Utils.IntervalMillis();
        MI2Utils.IntervalMillis actionTimer = new MI2Utils.IntervalMillis(2);
        public boolean itemTrans, payloadTrans;
        public static StringBuffer log = new StringBuffer();
        Queue<BuildPlan> plans = new Queue<>();

        public PopupTable customAIUITable = new PopupTable();

        public static PopupTable chooseContentTable = new PopupTable();

        //public int lastPathId = 0;
        //public float lastMoveX, lastMoveY;

        public LogicMode(){
            super();
            chooseContentTable.update(() -> chooseContentTable.keepInScreen());

            Events.on(EventType.WorldLoadEvent.class, e -> readCode(code.value));

            logicMode = this;
            bannedInstructions.clear();
            bannedInstructions.addAll(ControlI.class, WriteI.class, StopI.class, SetBlockI.class, SpawnUnitI.class, ApplyEffectI.class, SetRuleI.class, SetRateI.class, ExplosionI.class, SetFlagI.class, SpawnWaveI.class, SetPropI.class);
            btext = Iconc.blockWorldProcessor + "";
            bimg = Core.atlas.drawable("mi2-utilities-java-ui-customai");

            LogicMode.logicMode.codes = Core.settings.getJson("ai.logic.codes", Seq.class, LogicModeCode.class, () -> Seq.with(new LogicModeCode("" + Iconc.edit + Iconc.map, "jump 26 strictEqual init 2\n" +
                    "set brush.size 2\n" +
                    "set floor @air\n" +
                    "set ore @air\n" +
                    "set block @air\n" +
                    "set title \"TerraEditor\"\n" +
                    "set text.ipt \"Ipt\"\n" +
                    "set ipt 50\n" +
                    "print \"UI.info(title)\"\n" +
                    "print \"UI.row()\"\n" +
                    "print \"UI.info(text.ipt)\"\n" +
                    "print \"UI.field(ipt)\"\n" +
                    "print \"UI.row()\"\n" +
                    "print \"UI.choose(floor)\"\n" +
                    "print \"UI.info(floor)\"\n" +
                    "print \"UI.row()\"\n" +
                    "print \"UI.choose(ore)\"\n" +
                    "print \"UI.info(ore)\"\n" +
                    "print \"UI.row()\"\n" +
                    "print \"UI.info(brush.name)\"\n" +
                    "print \"UI.button(brush.type)\"\n" +
                    "print \"UI.row()\"\n" +
                    "print \"UI.info(brush.size.name)\"\n" +
                    "print \"UI.field(brush.size)\"\n" +
                    "set init 2\n" +
                    "set brush.size.name \"Radius\"\n" +
                    "sensor en @unit @shooting\n" +
                    "set brush.name \"suqare\"\n" +
                    "jump 30 equal brush.type 0\n" +
                    "set brush.name \"circle\"\n" +
                    "sensor tx @unit @shootX\n" +
                    "op add tx tx 0.5\n" +
                    "op idiv tx tx 1\n" +
                    "sensor ty @unit @shootY\n" +
                    "op add ty ty 0.5\n" +
                    "op idiv ty ty 1\n" +
                    "op sub x.min tx brush.size\n" +
                    "op idiv x.min x.min 1\n" +
                    "op add x.max x.min brush.size\n" +
                    "op add x.max x.max brush.size\n" +
                    "op sub y.min ty brush.size\n" +
                    "op idiv y.min y.min 1\n" +
                    "op add y.max y.min brush.size\n" +
                    "op add y.max y.max brush.size\n" +
                    "set x x.min\n" +
                    "op add x x 1\n" +
                    "set y y.min\n" +
                    "op add y y 1\n" +
                    "jump 53 equal brush.type 0\n" +
                    "op sub dx x tx\n" +
                    "op sub dy y ty\n" +
                    "op len d dx dy\n" +
                    "jump 57 greaterThan d brush.size\n" +
                    "effect lightBlock x y 0.5 %ffbd530f \n" +
                    "jump 57 notEqual en 1\n" +
                    "setblock floor floor x y @derelict 0\n" +
                    "setblock ore ore x y @derelict 0\n" +
                    "jump 47 lessThan y y.max\n" +
                    "setrate ipt\n" +
                    "jump 45 lessThan x x.max\n")));
            code = codes.first();
            readCode(code.value);

            Events.on(EventType.WorldLoadEvent.class, e-> {
                if(logicMode != null){
                    logicMode.readCode(logicMode.code.value);
                }
            });
        }

        @Override
        public void buildConfig(Table table){
            table.clear();
            super.buildConfig(table);
            table.table(t -> {
                t.table(this::buildChoose).growX();
                t.button("" + Iconc.add, textb, () -> {
                    var newCode = new LogicModeCode(String.valueOf(codes.size), "");
                    codes.add(newCode);
                    code = newCode;
                    saveCodes();
                    buildConfig(table);
                }).size(32f);
            }).growX();
            table.row();
            table.image().height(2f).growX().color(Color.white);
            table.row();

            table.table(t -> {
                t.name = "cfg";
                t.button("" + Iconc.edit, textb, () -> {
                    ui.showTextInput("Edit Logic AI Name", "Edit Logic AI Name", code.name, s -> {
                        if(!s.equals(code.name)){
                            code.name = s;
                            saveCodes();
                        }
                    });
                }).size(32f);
                t.label(() -> code.name).grow();
                t.button("" + Iconc.blockWorldProcessor, textb, () -> {
                    ui.logic.show(code.value, exec, true, s -> {
                        code.value = s;
                        this.readCode(code.value);
                        saveCodes();
                    });
                }).size(32f);
                t.button(Iconc.info + "", textb, () -> {
                    ui.showText("", Core.bundle.get("fullAI.help"), Align.left);
                }).size(32f);
                t.button(Iconc.cancel + "", textb, () -> ui.showConfirm("Confirm Delete:" + code.name, () -> {
                    int index = codes.indexOf(code) - 1;
                    if(index < 0) index = 0;
                    codes.remove(code);
                    saveCodes();
                    code = codes.get(index);
                    buildConfig(table);
                })).disabled(tb -> codes.size <= 1).size(32f).get().getLabel().setColor(Color.scarlet);
            }).grow();
            table.row();

            table.table(t -> {
                t.add("Message Log").left().color(Color.royal).growX();
                t.button("@ai.config.logic.ui", textbtoggle, () -> {
                    if(customAIUITable.shown){
                        customAIUITable.hide();
                    }else{
                        customAIUITable.popup();
                        customAIUITable.setPositionInScreen(Core.input.mouseX(), Core.input.mouseY());
                    }
                }).checked(tb -> customAIUITable.shown).size(80f, 32f);
            }).growX();

            table.row();
            table.pane(t -> {
                t.name = "log";
                t.image().color(Color.royal).growY().width(2f);
                t.labelWrap(() -> log).grow();
            }).grow().maxHeight(200f);
        }

        public void buildChoose(Table table){
            int i = 0;
            for(var lmc : codes){
                table.button(lmc.name.substring(0, Math.min(lmc.name.length(), 6)), textbtoggle, () -> {
                    code = lmc;
                    readCode(code.value);
                }).with(funcSetTextb).width(60f).height(28f).margin(2f).with(tb -> {
                    tb.getLabel().setFontScale(0.8f);
                    tb.update(() -> {
                        tb.setChecked(code == lmc);
                        tb.setText(lmc.name);
                    });
                });
                if(i++ > 2){
                    i = 0;
                    table.row();
                }
            }
        }

        @Override
        public void act(){
            exec.vars[exec.iptIndex].numval = instructionsPerTick;
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
                    moveAction(ai.moveX, ai.moveY, ai.control == LUnitControl.move ? 1f : Math.max(ai.moveRad, 1f), false);
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

        public void saveCodes(){
            Core.settings.putJson("ai.logic.codes", LogicModeCode.class, codes);
        }

        public void readCode(String str){
            code.value = str;
            LAssembler asm = LAssembler.assemble(str, true);
            asm.putConst("@mapw", world.width());
            asm.putConst("@maph", world.height());
            asm.putConst("@links", exec.links.length);
            asm.putConst("@ipt", instructionsPerTick);
            exec.load(asm);
            exec.privileged = true;
            customAIUITable.clear();
            customAIUITable.touchable = Touchable.enabled;
            customAIUITable.margin(2f);
            customAIUITable.background(Styles.black3);
            customAIUITable.addDragMove();
            customAIUITable.addCloseButton(20f);
            customAIUITable.add("@ai.config.logic.ui").height(20f).row();
            customAIUITable.update(() -> customAIUITable.keepInScreen());
            customAIUITable.defaults().size(80, 32);
        }

        public void updatePlayerActionTimer(){
            itemTrans = true;
            payloadTrans = true;
        }

        public boolean isLocalSandbox(){
            return !net.client() || state.rules.mode() == Gamemode.sandbox;
        }

        public boolean tryRunOverwrite(LInstruction inst){
            if(inst instanceof SetRateI sr){
                instructionsPerTick = exec.numi(sr.amount);
                return true;
            }else if(inst instanceof ControlI li){
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
            }else if(inst instanceof PrintI printI){
                return printUI(printI);
            }

            return false;
        }

        public boolean printUI(PrintI inst){
            //format: UI.type(var)
            var text = exec.var(inst.value);
            var str = text.isobj && inst.value != 0 ? PrintI.toString(text.objval) : String.valueOf(text.numval);
            if(!str.endsWith(")")) return false;
            String[] blocks = str.substring(0, str.length() - 1).split("\\.|\\(", 3);

            if(blocks.length < 2 || !blocks[0].equals("UI")) return false;
            var type = blocks[1];
            if(type.equals("row")){
                customAIUITable.row();
                return true;
            }else if(type.equals("clear")){
                customAIUITable.clearChildren();
                customAIUITable.addCloseButton(20f);
                customAIUITable.add("@ai.config.logic.ui").height(20f).row();
                return true;
            }
            if(blocks.length < 3) return false;
            var targetName = blocks[2];
            for(int i = 0; i < exec.vars.length; i++){
                if(exec.var(i).name.equals(targetName)){
                    int tgt = i;
                    if(customAIUITable.getChildren().size > 60) return false;//no too many uis
                    switch(type){
                        case "button" -> customAIUITable.button(targetName, textbtoggle, () -> exec.setbool(tgt, !exec.bool(tgt))).update(tb -> tb.setChecked(exec.bool(tgt)));
                        case "field" -> customAIUITable.field(String.valueOf(exec.num(tgt)), TextField.TextFieldFilter.floatsOnly, s -> exec.setnum(tgt, Strings.parseDouble(s, 0)));
                        case "choose" -> {
                            if(exec.var(tgt).isobj && !(exec.obj(tgt) instanceof MappableContent)) return false;
                            customAIUITable.button(targetName, textb, () -> {
                                chooseContentTable.clear();
                                chooseContentTable.addDragMove();
                                chooseContentTable.addCloseButton();
                                chooseContentTable.visible(() -> state.isGame());
                                buildTable(chooseContentTable, new Seq<UnlockableContent>().add(content.items()).add(content.liquids()).add(content.statusEffects()).add(content.blocks()).add(content.units()), () -> exec.obj(tgt) instanceof UnlockableContent uc ? uc : null, content -> exec.setobj(tgt, content), false, 8, 8);
                                chooseContentTable.popup();
                                chooseContentTable.snapTo(customAIUITable);
                            });
                        }
                        case "info" -> {

                            customAIUITable.add("").fill().with(b -> {
                                b.setFontScale(0.7f);
                                b.clicked(() -> b.cullable = !b.cullable);
                                b.update(() -> {
                                    b.setText(PrintI.toString(exec.obj(tgt)));
                                    b.setColor(b.cullable ? Color.cyan : Color.white);
                                    if(b.hasMouse()) HoverTopTable.hoverInfo.setHovered(exec.obj(tgt));
                                    if(b.cullable){
                                        if(exec.obj(tgt) instanceof Posc posc && control.input instanceof InputOverwrite ipo){
                                            ipo.pan(true, MI2UTmp.v1.set(posc));
                                        }
                                    }
                                });
                            });
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        public static <T extends UnlockableContent> void buildTable(Table table, Seq<T> items, Prov<T> holder, Cons<T> consumer, boolean closeSelect, int rows, int columns){
            ButtonGroup<ImageButton> group = new ButtonGroup<>();
            group.setMinCheckCount(0);
            Table cont = new Table().top();
            cont.defaults().size(40);

            Table main = new Table().background(Styles.black6);

            Runnable rebuild = () -> {
                group.clear();
                cont.clearChildren();

                int i = 0;
                for(T item : items){
                    ImageButton button = cont.button(Tex.whiteui, Styles.clearNoneTogglei, Mathf.clamp(item.selectionSize, 0f, 40f), () -> {
                        if(closeSelect) control.input.config.hideConfig();
                    }).tooltip(item.localizedName).group(group).get();
                    button.changed(() -> consumer.get(button.isChecked() ? item : null));
                    button.getStyle().imageUp = new TextureRegionDrawable(item.uiIcon);
                    button.update(() -> button.setChecked(holder.get() == item));

                    if(i++ % columns == (columns - 1)){
                        cont.row();
                    }
                }
            };

            rebuild.run();

            ScrollPane pane = new ScrollPane(cont, Styles.smallPane);
            pane.setScrollingDisabled(true, false);

            pane.setOverscroll(false, false);
            main.add(pane).maxHeight(40 * rows);
            table.top().add(main);
        }

        public static class LogicModeCode{
            String name;
            String value;
            public LogicModeCode(){}
            public LogicModeCode(String n, String v){
                name = n;
                value = v;
            }
        }
    }
}