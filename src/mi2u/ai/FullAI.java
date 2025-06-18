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
import mindustry.world.meta.*;

import static mi2u.MI2UVars.*;
import static mindustry.Vars.*;

public class FullAI extends AIController{
    public Seq<Mode> modes = new Seq<>();

    boolean unlockUnitBuild = false, cacheRuleLogicUnitBuild;

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


        ui.logic.shown(() -> {
            if(unlockUnitBuild){
                cacheRuleLogicUnitBuild = state.rules.logicUnitBuild;
                state.rules.logicUnitBuild = true;
            }
        });
        ui.logic.hidden(() -> {
            if(unlockUnitBuild) state.rules.logicUnitBuild = cacheRuleLogicUnitBuild;
        });
    }

    @Override
    public void updateUnit(){
        if(unit == null) return;
        if(control.input instanceof InputOverwrite inp){
            inp.clear();
            modes.each(mode -> {
                if(mode.enable) mode.act();
            });
        }
    }

    /**unit actions can be covered by the lasted related mode. Executed after each mode acted.*/
    public void moveAction(Position target, float radius, boolean checkWithin){
        moveAction(target.getX(), target.getY(), radius, checkWithin);
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
        Interval timer = new Interval(4);

        public Mode(){
            btext = Iconc.units + "";
        }
        /** override it. enable auto checked. */
        public void act(){}

        public void buildConfig(Table table){}
    }

    public class BaseMineMode extends Mode{
        /** 提升可读性，下同 */
        static short timerTargetItem = 0, timerFindOre = 1, timerTransferItem = 2;
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
                if(timer.get(timerTargetItem, 60 * 4) || targetItem == null){
                    targetItem = list.min(i -> unit.canMine(i), i -> core.items.get(i));
                }
                //core full of the target item, do nothing
                if(targetItem != null && core.acceptStack(targetItem, 1, unit) == 0){
                    unit.clearItem();
                    unit.mineTile = null;
                    return;
                }
                //if inventory is full, drop it off.
                if(targetItem != null){
                    if(!unit.acceptsItem(targetItem)){
                        mining = false;
                    }

                    if(timer.get(timerFindOre, 120f)){
                        //Anuke终于做一回人了
                        ore = unit.type.mineWalls ? indexer.findClosestWallOre(unit, targetItem) : indexer.findClosestOre(unit, targetItem);
                    }

                    if(ore != null){
                        moveAction(ore, 40f, true);
                        if(unit.within(ore, unit.type.mineRange)){
                            unit.mineTile = ore;
                        }
                    }
                }
            }else{
                unit.mineTile = null;
                if(!unit.hasItem()){
                    mining = true;
                    return;
                }

                if(unit.within(core, itemTransferRange / 1.1f)){
                    if(timer.get(timerTransferItem, 120f)){
                        control.input.droppingItem = true;
                        control.input.tryDropItems(core, unit.aimX, unit.aimY);
                    }else{
                        shootAction(MI2UTmp.v1.set(core).lerpDelta(Mathf.range(-12f, 12f), Mathf.range(-8f, 8f), 0.1f), true);
                    }
                    mining = true;
                }

                moveAction(core, itemTransferRange / 2f, true);
            }
        }

        @Override
        public void buildConfig(Table table) {
            super.buildConfig(table);
            table.pane(p -> {
                int i = 0;
                for(var item : content.items()){
                    if(!content.blocks().contains(b -> b.itemDrop == item)) continue;
                    boolean floor = indexer.hasOre(item), wall = indexer.hasWallOre(item);
                    p.button(b -> {
                        var icon = new Image(item.uiIcon);
                        icon.setFillParent(true);
                        var label = new Label(() -> ((floor ? "F" : "") + (wall ? "w" : "")));
                        label.setAlignment(Align.bottomRight);
                        label.setFontScale(0.7f);
                        b.stack(icon, label);
                    }, textbtoggle, () -> {
                        if(list.contains(item)){
                            list.remove(item);
                        }else {
                            list.add(item);
                        }
                    }).fill().margin(4f).disabled(!floor && !wall).update(b -> b.setChecked(list.contains(item)));
                    i++;
                    if(i >= 8){
                        i = 0;
                        p.row();
                    }
                }
            }).maxHeight(300f).with(p -> p.setFadeScrollBars(false));
        }
    }

    public class AutoBuildMode extends Mode{
        short timerCobuild = 0, timerRebuild = 1, timerCheck = 2;
        public boolean rebuild = false, follow = true;
        final BuildPlan autoPlan = new BuildPlan();

        public AutoBuildMode(){
            btext = Iconc.unitPoly + "";
        }

        @Override
        public void act(){
            if(!control.input.isBuilding) return;
            if(!unit.canBuild()) return;
            //help others building, catching the closest plan to co-op.
            if(follow && timer.get(timerCobuild, 15f) && (unit.plans().isEmpty() || unit.buildPlan().samePos(autoPlan))){
                if(autoPlan.isDone()) unit.validatePlans();
                //搜寻其他玩家的建造计划
                for(var player : Groups.player){
                    if(player.unit() == null || player.team() != unit.team) continue;

                    var other = player.unit();
                    if(other != unit && other.canBuild() && other.activelyBuilding() && other.buildPlan() != null){
                        BuildPlan plan = other.buildPlan();
                        //潜在目标只能是已经在建的建筑
                        if(other.buildPlan().build() instanceof ConstructBlock.ConstructBuild c){
                            //判断能否准时赶到；是否离当前目标更近
                            if(((c.dst(unit) - buildingRange) / (unit.type.speed * 60) < c.buildCost * (1f - c.progress()) * 0.9f) || (unit.buildPlan() != null && unit.dst(c) < unit.dst(unit.buildPlan()))){
                                //检查核心资源
                                boolean req = true;
                                for(var itemStack : plan.block.requirements){
                                    if(player.team().core() != null && !player.team().core().items.has(itemStack.item)){
                                        req = false;
                                        break;
                                    }
                                }

                                if(req){
                                    autoPlan.config = plan.config;
                                    unit.plans.addFirst(autoPlan.set(plan.x, plan.y, plan.rotation, plan.block));
                                }
                            }
                        }

                        //发现其他玩家有矛盾意见时，移除该目标
                        if(unit.buildPlan() != null && plan != null && plan.breaking != unit.buildPlan().breaking && plan.samePos(unit.buildPlan())){
                            unit.plans.removeFirst();
                        }
                    }
                }
            }

            if(rebuild && unit.plans().isEmpty() && timer.get(timerRebuild, 45f) && !unit.team.data().plans.isEmpty()){
                float minDst = Float.MAX_VALUE;
                Teams.BlockPlan plan = null;
                for(var p : unit.team.data().plans){
                    if(unit.dst(p.x * tilesize, p.y * tilesize) < minDst){
                        minDst = unit.dst(p.x * tilesize, p.y * tilesize);
                        plan = p;
                    }
                }

                if(plan != null && world.tile(plan.x, plan.y).block() == plan.block){
                    state.teams.get(player.team()).plans.remove(plan);
                }else{
                    unit.plans.addFirst(new BuildPlan(plan.x, plan.y, plan.rotation, plan.block, plan.config));
                }
            }

            if(unit.plans().isEmpty()) return;
            boostAction(true);
            float minDst = Float.MAX_VALUE, tmp;
            BuildPlan min = unit.plans.first();
            if(unit.plans.size < 128){
                for(var bp : unit.plans){
                    tmp = MI2UTmp.v1.set(bp).dst(unit);
                    if(tmp < minDst){
                        min = bp;
                        minDst = tmp;
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
        public boolean itemTrans, payloadTrans;
        public static StringBuffer log = new StringBuffer();
        Queue<BuildPlan> plans = new Queue<>();

        public PopupTable customAIUITable = new PopupTable();

        public static PopupTable chooseContentTable = new PopupTable();

        short timerUpdMovement = 0, timerMove = 1, timerShoot = 2, timerTransItemPayload = 3;

        public LogicMode(){
            super();
            chooseContentTable.update(() -> chooseContentTable.keepInScreen());

            Events.on(EventType.WorldLoadEvent.class, e -> readCode(code.value));

            logicMode = this;
            bannedInstructions.clear();
            bannedInstructions.addAll(WriteI.class, ControlI.class, StopI.class, SetBlockI.class, SpawnUnitI.class, ApplyEffectI.class, SetWeatherI.class, SpawnWaveI.class, SetRuleI.class, ExplosionI.class, SetRateI.class, SyncI.class, SetFlagI.class, SetPropI.class, LocalePrintI.class);
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
                    ui.showTextInput(Iconc.edit + "Edit Logic AI Name", "Edit Logic AI Name", code.name, s -> {
                        if(!s.equals(code.name)){
                            code.name = s;
                            saveCodes();
                        }
                    });
                }).size(32f);
                t.label(() -> code.name).grow();
                t.button("" + Iconc.blockWorldProcessor, textb, () -> {
                    Runnable shower = () -> {
                        ui.logic.show(code.value, exec, true, s -> {
                            code.value = s;
                            this.readCode(code.value);
                            saveCodes();
                        });
                    };

                    if(!state.rules.logicUnitBuild){
                        ui.showCustomConfirm("", "@ai.config.logic.unitBuildWarning", "" + Iconc.ok, "" + Iconc.cancel, () -> {
                            unlockUnitBuild = true;
                            shower.run();
                        }, () -> {
                            unlockUnitBuild = false;
                            shower.run();
                        });
                    }else{
                        shower.run();
                    }
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
            exec.ipt.numval = instructionsPerTick;
            exec.unit.constant = false;
            var ctrl = unit.controller();
            unit.controller(ai);

            //TODO 可调运输冷却
            if(timer.get(timerTransItemPayload, 1)){
                itemTrans = true;
                payloadTrans = true;
            }

            if(timer.get(timerUpdMovement, 5)){
                ai.targetTimer = 0f;
                ai.controlTimer = LogicAI.logicControlTimeout;
                ai.updateMovement();
            }

            plans.clear();
            if(unit.plans != null) unit.plans.each(bp -> plans.add(bp));
            for(int i = 0; i < Mathf.clamp(instructionsPerTick, 1, 2000); i++){
                if(exec.instructions.length == 0) break;
                exec.unit.setobj(unit);
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

            if(!timer.check(timerMove, LogicAI.logicControlTimeout / 60)){
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

            if(!timer.check(timerShoot, LogicAI.logicControlTimeout / 60)){
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
            exec.load(asm);
            asm.putConst("@links", exec.links.length);
            asm.putConst("@ipt", instructionsPerTick);
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

        public boolean isLocalSandbox(){
            return !net.client() || state.rules.mode() == Gamemode.sandbox;
        }

        public boolean tryRunOverwrite(LInstruction inst){
            if(inst instanceof SetRateI sr){
                instructionsPerTick = sr.amount.numi();
                return true;
            }else if(inst instanceof ControlI li){
                if(!player.dead() && li.target.obj() instanceof Building b && (isLocalSandbox() || b.team == exec.team)){
                    if(li.type == LAccess.config){
                        b.configured(player.unit(), li.p1);
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
                        timer.get(timerShoot, 1);
                        return false;
                    }
                    case move, pathfind, autoPathfind, boost -> {
                        timer.get(timerMove, 1);
                        return false;
                    }
                    case idle -> {
                        timer.reset(timerMove, 60);
                        return false;
                    }
                    case stop -> {
                        timer.reset(timerShoot, 60);
                        return false;
                    }
                    case itemTake -> {
                        if(!(li.p2.obj() instanceof Item item)) return false;
                        if(!itemTrans || player.unit() == null || !player.unit().acceptsItem(item)) return false;
                        Building build = li.p1.building();
                        if(build != null && build.team == unit.team && build.isValid() && build.items != null && unit.within(build, itemTransferRange + build.block.size * tilesize/2f)){
                            Call.requestItem(player, build, item, li.p3.numi());
                            itemTrans = false;
                        }
                        return true;
                    }
                    case itemDrop -> {
                        if(!itemTrans || player.unit() == null || player.unit().stack.amount == 0) return false;
                        Building build = li.p1.building();
                        if(build != null && unit.within(build, itemTransferRange + build.block.size * tilesize/2f)){
                            control.input.droppingItem = true;
                            control.input.tryDropItems(build, 0f, 0f);
                            control.input.droppingItem = false;
                            itemTrans = false;
                        }else if(li.p1.obj() == Blocks.air){
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
            var str = inst.value.isobj && inst.value.numi() != 0 ? PrintI.toString(inst.value.obj()) : String.valueOf(inst.value.numval);
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

            @Nullable LVar target = exec.optionalVar(blocks[2]);

            if(target == null) return false;

            if(customAIUITable.getChildren().size > 60) return false;//no too many uis
            switch(type){
                case "button" -> customAIUITable.button(target.name, textbtoggle, () -> target.setbool(!target.bool())).update(tb -> tb.setChecked(target.bool()));

                case "field" -> customAIUITable.field(String.valueOf(target.num()), TextField.TextFieldFilter.floatsOnly, s -> target.setnum(Strings.parseDouble(s, 0)));

                case "choose" -> {
                    if(!(target.obj() instanceof MappableContent)) return false;
                    customAIUITable.button(target.name, textb, () -> {
                        chooseContentTable.clear();
                        chooseContentTable.addDragMove();
                        chooseContentTable.addCloseButton();
                        chooseContentTable.visible(() -> state.isGame());
                        buildTable(chooseContentTable, new Seq<UnlockableContent>().add(content.items()).add(content.liquids()).add(content.statusEffects()).add(content.blocks()).add(content.units()), () -> target.obj() instanceof UnlockableContent uc ? uc : null, content -> target.setobj(content), false, 8, 8);
                        chooseContentTable.popup();
                        chooseContentTable.snapTo(customAIUITable);
                    });
                }

                case "info" -> {
                    customAIUITable.add("").fill().with(b -> {
                        b.setFontScale(0.7f);
                        b.clicked(() -> b.cullable = !b.cullable);
                        b.update(() -> {
                            b.setText(PrintI.toString(target.obj()));
                            b.setColor(b.cullable ? Color.cyan : Color.white);
                            if(b.hasMouse()) HoverTopTable.hoverInfo.setHovered(target.obj());
                            if(b.cullable && (target.obj() instanceof Posc posc)) InputUtils.panStable(posc);
                        });
                    });
                }
            }

            return true;
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
            /** Serialization-required constructor. DO NOT DELETE.   */
            public LogicModeCode(){}
            public LogicModeCode(String n, String v){
                name = n;
                value = v;
            }
        }
    }
}