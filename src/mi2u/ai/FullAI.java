package mi2u.ai;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.*;
import mi2u.input.*;
import mi2u.io.*;
import mi2u.ui.*;
import mi2u.ui.elements.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.logic.*;
import mindustry.logic.LExecutor.*;
import mindustry.type.*;
import mindustry.type.weapons.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.logic.*;

import static mi2u.MI2UVars.*;
import static mindustry.Vars.*;

public class FullAI extends AIController{
    /** 所有可添加的AI的元数据 */
    public static Seq<Mode.ModeMeta> all = new Seq<>();
    /** 所有Mode类的元数据 */
    public static ObjectMap<Class, Mode.ModeMeta> meta = new ObjectMap<>();
    public Seq<Mode> modes = new Seq<>();

    static boolean unlockUnitBuild = false, cacheRuleLogicUnitBuild;

    public FullAI(){
        super();

        register(BaseMineMode.class, new Mode.ModeMeta(BaseMineMode.class, Core.bundle.get("ai.preset.baseminemode"), Core.bundle.get("ai.preset.baseminemode.intro"), new TextureRegionDrawable(UnitTypes.mono.uiIcon), BaseMineMode::new));

        register(AutoBuildMode.class, new Mode.ModeMeta(AutoBuildMode.class, Core.bundle.get("ai.preset.autobuildmode"), Core.bundle.get("ai.preset.autobuildmode.intro"), new TextureRegionDrawable(UnitTypes.poly.uiIcon), AutoBuildMode::new));

        register(AutoTargetMode.class, new Mode.ModeMeta(AutoTargetMode.class, Core.bundle.get("ai.preset.autotargetmode"), Core.bundle.get("ai.preset.autotargetmode.intro"), Core.atlas.drawable("mi2-utilities-java-ui-shoot"), AutoTargetMode::new));

        register(CenterFollowMode.class, new Mode.ModeMeta(CenterFollowMode.class, Core.bundle.get("ai.preset.centerfollowmode"), Core.bundle.get("ai.preset.centerfollowmode.intro"), Core.atlas.drawable("mi2-utilities-java-ui-centermove"), CenterFollowMode::new));

        register(LogicMode.class, new Mode.ModeMeta(LogicMode.class, Core.bundle.get("ai.preset.logicmode"), Core.bundle.get("ai.preset.logicmode.intro"), Core.atlas.drawable("mi2-utilities-java-ui-customai"), LogicMode::new));

        loadPresets();
        loadModes();

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

    public void loadModes(){
        modes = Core.settings.getJson("MI2U.ai.modes", Seq.class, Seq::new);
    }

    public void saveModes(){
        Core.settings.putJson("MI2U.ai.modes", modes);
    }

    //加载assest下所有世处AI的预置脚本
    public void loadPresets(){
        for(var mai : Presets.all){
            String code = mai.value;
            String presetName = Core.bundle.get("ai.preset.logicmode.preset." + mai.name);
            String presetIntro = Core.bundle.get("ai.preset.logicmode.preset." + mai.name + ".intro");
            if(code != null) register(LogicMode.class, new Mode.ModeMeta(LogicMode.class, presetName, presetIntro, null, () -> {
                var m = new LogicMode();
                m.readCode(code);
                m.handle = presetName;
                return m;
            }));
        }
    }

    public void register(Class clazz, Mode.ModeMeta m){
        all.add(m);
        if(!meta.containsKey(clazz)) meta.put(clazz, m);
        SettingHandler.registerJsonClass(clazz);
    }

    public void modeFlush(){
        modes.each(mode -> mode.ai = this);
    }

    @Override
    public void updateUnit(){
        modeFlush();
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

    public static class Mode{
        public boolean enable = false;
        public String handle = "";

        transient FullAI ai;
        transient public boolean configUIExpand = false;
        transient Interval timer = new Interval(4);

        public Mode(){}

        /** override it. enable auto checked. */
        public void act(){}

        public void buildTitle(Table table){
            table.table(tt -> {
                tt.defaults().pad(2f);
                tt.button("" + Iconc.up, textb, () -> {
                    int i = ai.modes.indexOf(this);
                    ai.modes.swap(i, Math.max(i - 1, 0));
                    aiMindow.rebuild();
                }).disabled(ai.modes.indexOf(this) == 0).size(buttonSize);

                tt.button("" + Iconc.down, textb, () -> {
                    int i = ai.modes.indexOf(this);
                    ai.modes.swap(i, Math.min(i + 1, ai.modes.size - 1));
                    aiMindow.rebuild();
                }).disabled(ai.modes.indexOf(this) == ai.modes.size - 1).size(buttonSize);

                tt.button("" + Iconc.edit, textbtoggle, () -> configUIExpand = !configUIExpand).checked(tb -> configUIExpand).size(32f);

                tt.add(new MCollapser(t -> {
                    t.button("" + Iconc.star, textb, () -> {
                        ui.showTextInput("Edit Logic AI Name", "Edit Logic AI Name", handle, s -> {
                            if(!s.equals(handle)){
                                handle = s;
                            }
                        });
                    }).size(buttonSize);
                    t.button("[scarlet]" + Iconc.cancel, textb, () -> {
                        ui.showConfirm("Remove This AI: " + name(), () -> {
                            ai.modes.remove(this);
                            aiMindow.rebuild();
                        });
                    }).size(buttonSize);
                }, true).setCollapsed(true, () -> !configUIExpand).setDirection(true, false).setDuration(0.1f));

                tt.table(ent -> {
                    var icon = meta.get(this.getClass()).icon;
                    if(icon == null){
                        ent.image().size(32f);
                    }else{
                        ent.image(icon).size(32f).scaling(Scaling.fit);
                    }
                    ent.label(() -> (enable ? (" [accent]" + Iconc.play) : (" [gray]" + Iconc.pause)) + " " + name()).growX();
                }).left().growX().get().clicked(() -> enable = !enable);

            }).growX();
        }

        public void buildConfig(Table table){}

        public static Stack timerIconStack(Prov<String> icon, Boolp charged, Floatp prov){
            Stack stack = new Stack();
            var i = new Label(() -> (charged.get() ? "[lightgray]" : "[darkgray]") + icon.get());
            stack.add(i);
            var l = new Label(() -> charged.get() ? Strings.autoFixed(prov.get(), 1) : "");
            l.setFillParent(true);
            l.setAlignment(Align.bottomRight);
            l.setWrap(true);
            l.setFontScale(0.6f);
            stack.add(l);
            stack.setHeight(32f);
            return stack;
        }

        public String name(){
            return handle == null || handle.isEmpty() ? meta.get(this.getClass()).name : handle;
        }

        public static class ModeMeta{
            public Class<?> clazz;
            public String name, intro;
            public @Nullable Drawable icon;
            public Prov<Mode> prov;
            public ModeMeta(Class<?> clazz, String name, String intro, @Nullable Drawable icon, Prov<Mode> prov){
                this.clazz = clazz;
                this.name = name;
                this.intro = intro;
                this.icon = icon;
                this.prov = prov;
            }
        }
    }

    public static class BaseMineMode extends Mode{
        /** 提升可读性，下同 */
        static short timerTargetItem = 0, timerFindOre = 1, timerTransferItem = 2;
        public Seq<Item> list = new Seq<>();
        transient boolean mining;
        transient Item targetItem;
        transient Tile ore;

        public BaseMineMode(){
            list.add(Items.copper, Items.lead);
        }

        @Override
        public void act(){
            Building core = ai.unit.closestCore();
            if(!(ai.unit.canMine()) || core == null) return;
            ai.boostAction(true);
            if(ai.unit.mineTile != null && !ai.unit.mineTile.within(ai.unit, ai.unit.type.mineRange)){
                ai.unit.mineTile = null;
            }
            if(mining){
                if(timer.get(timerTargetItem, 60 * 4) || targetItem == null){
                    targetItem = list.min(i -> ai.unit.canMine(i) && (indexer.hasOre(i) || indexer.hasWallOre(i)), i -> core.items.get(i));
                }
                //core full of the target item, do nothing
                if(targetItem != null && core.acceptStack(targetItem, 1, ai.unit) == 0){
                    ai.unit.clearItem();
                    ai.unit.mineTile = null;
                    return;
                }
                //if inventory is full, drop it off.
                if(targetItem != null){
                    if(!ai.unit.acceptsItem(targetItem)){
                        mining = false;
                    }

                    if(timer.get(timerFindOre, 60 * 2)){
                        //Anuke终于做一回人了
                        ore = ai.unit.type.mineWalls ? indexer.findClosestWallOre(ai.unit, targetItem) : indexer.findClosestOre(ai.unit, targetItem);
                    }

                    if(ore != null){
                        ai.moveAction(ore, 40f, true);
                        if(ai.unit.within(ore, ai.unit.type.mineRange)){
                            ai.unit.mineTile = ore;
                        }
                    }
                }
            }else{
                ai.unit.mineTile = null;
                if(!ai.unit.hasItem()){
                    mining = true;
                    return;
                }

                if(ai.unit.within(core, itemTransferRange / 1.1f)){
                    if(timer.get(timerTransferItem, 120f)){
                        control.input.droppingItem = true;
                        control.input.tryDropItems(core, ai.unit.aimX, ai.unit.aimY);
                    }
                    mining = true;
                }

                ai.moveAction(core, itemTransferRange / 2f, true);
            }
        }

        @Override
        public void buildConfig(Table table){
            table.table(t -> {
                t.defaults().pad(2f).minWidth(32f);
                t.add(timerIconStack(() -> targetItem == null ? ("" + Iconc.none) : (targetItem.hasEmoji() ? targetItem.emoji() : targetItem.localizedName), () -> targetItem == null || !timer.check(timerTargetItem, 60 * 4), () -> (60 * 4 - timer.getTime(timerTargetItem)) / 60f));
                t.add(timerIconStack(() -> "" + Iconc.zoom, () -> ai.unit != null && ai.unit.mineTile != null, () -> (60 * 2 - timer.getTime(timerFindOre)) / 60f));
                t.add(timerIconStack(() -> "" + Iconc.upload, () -> !timer.check(timerTransferItem, 60 * 2), () -> (60 * 2 - timer.getTime(timerTransferItem)) / 60f));
            }).growX().row();

            table.table(p -> {
                int i = 0;
                for(var item : content.items()){
                    if(!content.blocks().contains(b -> b.itemDrop == item)) continue;
                    boolean floor = indexer.hasOre(item), wall = indexer.hasWallOre(item);
                    p.button(b -> {
                        var icon = new Image(item.uiIcon);
                        icon.setFillParent(true);
                        var label = new Label(() -> ((floor ? "F" : "") + (wall ? "W" : "")));
                        label.setAlignment(Align.bottomRight);
                        label.setFontScale(0.7f);
                        b.stack(icon, label);
                    }, textbtoggle, () -> {
                        if(list.contains(item)){
                            list.remove(item);
                        }else {
                            list.add(item);
                        }
                    }).fill().margin(2f).checked(b -> list.contains(item));
                    i++;
                    if(i >= 10){
                        i = 0;
                        p.row();
                    }
                }
            });
        }
    }

    public static class AutoBuildMode extends Mode{
        static short timerCobuild = 0, timerRebuild = 1;
        public boolean rebuild = false, follow = true;
        transient final BuildPlan autoPlan = new BuildPlan();

        public AutoBuildMode(){}

        @Override
        public void act(){
            if(!control.input.isBuilding) return;
            if(!ai.unit.canBuild()) return;
            //help others building, catching the closest plan to co-op.
            if(follow && timer.get(timerCobuild, 15f) && (ai.unit.plans().isEmpty() || ai.unit.buildPlan().samePos(autoPlan))){
                if(autoPlan.isDone()) ai.unit.validatePlans();
                //搜寻其他玩家的建造计划
                for(var player : Groups.player){
                    if(player.unit() == null || player.team() != ai.unit.team) continue;

                    var other = player.unit();
                    if(other != ai.unit && other.canBuild() && other.activelyBuilding() && other.buildPlan() != null){
                        BuildPlan plan = other.buildPlan();
                        //潜在目标只能是已经在建的建筑
                        if(other.buildPlan().build() instanceof ConstructBlock.ConstructBuild c){
                            //判断能否准时赶到；是否离当前目标更近
                            if(((c.dst(ai.unit) - buildingRange) / (ai.unit.type.speed * 60) < c.buildCost * (1f - c.progress()) * 0.9f) || (ai.unit.buildPlan() != null && ai.unit.dst(c) < ai.unit.dst(ai.unit.buildPlan()))){
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
                                    if(!ai.unit.plans.contains(autoPlan)) ai.unit.plans.addFirst(autoPlan.set(plan.x, plan.y, plan.rotation, plan.block));
                                }
                            }
                        }

                        //发现其他玩家有矛盾意见时，移除该目标
                        if(ai.unit.buildPlan() != null && plan != null && plan.breaking != ai.unit.buildPlan().breaking && plan.samePos(ai.unit.buildPlan())){
                            ai.unit.plans.removeFirst();
                        }
                    }
                }
            }

            if(rebuild && ai.unit.plans().isEmpty() && timer.get(timerRebuild, 45f) && !ai.unit.team.data().plans.isEmpty()){
                float minDst = Float.MAX_VALUE;
                Teams.BlockPlan plan = null;
                for(var p : ai.unit.team.data().plans){
                    if(ai.unit.dst(p.x * tilesize, p.y * tilesize) < minDst){
                        minDst = ai.unit.dst(p.x * tilesize, p.y * tilesize);
                        plan = p;
                    }
                }

                if(plan != null && world.tile(plan.x, plan.y).block() == plan.block){
                    state.teams.get(player.team()).plans.remove(plan);
                }else{
                    ai.unit.plans.addFirst(new BuildPlan(plan.x, plan.y, plan.rotation, plan.block, plan.config));
                }
            }

            if(ai.unit.plans().isEmpty()) return;
            ai.boostAction(true);
            float minDst = Float.MAX_VALUE, tmp;
            BuildPlan min = ai.unit.plans.first();
            if(ai.unit.plans.size < 128){
                for(var bp : ai.unit.plans){
                    tmp = MI2UTmp.v1.set(bp).dst(ai.unit);
                    if(tmp < minDst){
                        min = bp;
                        minDst = tmp;
                    }

                    if(tmp < buildingRange) break;
                }
            }
            ai.moveAction(min, buildingRange / 1.4f, true);
        }

        @Override
        public void buildConfig(Table table){
            table.table(t -> {
                t.defaults().growX();
                t.button("@ai.preset.autobuildmode.rebuild", textbtoggle, () -> {
                    rebuild = !rebuild;
                }).update(b -> b.setChecked(rebuild)).with(funcSetTextb);
                t.button("@ai.preset.autobuildmode.support", textbtoggle, () -> follow = !follow).update(b -> b.setChecked(follow)).with(funcSetTextb);
            }).growX();
        }
    }

    public static class AutoTargetMode extends Mode{
        public boolean attack = true, heal = true;

        public AutoTargetMode(){}

        @Override
        public void act(){
            if(Core.input.keyDown(Binding.select)) return;

            if(timer.get(0, 30f)){
                ai.target = null;
                float range = ai.unit.hasWeapons() ? ai.unit.range() : ai.unit instanceof BlockUnitUnit build ? build.tile() instanceof Turret.TurretBuild tb ? tb.range() : 0f : 0f;
                if(attack) ai.target = Units.closestTarget(ai.unit.team, ai.unit.x, ai.unit.y, range, u -> u.checkTarget(ai.unit.type.targetAir, ai.unit.type.targetGround), u -> ai.unit.type.targetGround);
                if(heal && (ai.unit.type.canHeal || ai.unit.type.weapons.contains(w -> w instanceof RepairBeamWeapon)) && ai.target == null){
                    ai.target = Geometry.findClosest(ai.unit.x, ai.unit.y, indexer.getDamaged(ai.unit.team));
                    if(ai.target != null && !ai.unit.within(ai.target, range)){
                        ai.target = null;
                    }
                }
            }

            if(ai.target != null){
                Vec2 intercept = Predict.intercept(ai.unit, ai.target, ai.unit.hasWeapons() ? ai.unit.type.weapons.first().bullet.speed : 0f);
                ai.shootAction(intercept, true);

            }else{
                ai.shootAction(MI2UTmp.v1.set(player.mouseX, player.mouseY), false);
            }
        }

        @Override
        public void buildConfig(Table table){
            table.table(t -> {
                t.defaults().growX();
                t.button("@ai.preset.autotargetmode.attack", textbtoggle, () -> attack = !attack).update(b -> b.setChecked(attack)).with(funcSetTextb);
                t.button("@ai.preset.autotargetmode.heal", textbtoggle, () -> heal = !heal).update(b -> b.setChecked(heal)).with(funcSetTextb);
            }).growX();
        }
    }

    public static class CenterFollowMode extends Mode{
        public CenterFollowMode(){}

        @Override
        public void act(){
            if(ai.unit.dead || !ai.unit.isValid()) return;
            if(enable){
                if(mobile){
                    ai.boostAction(true);
                    ai.moveAction(ai.unit, 8f, true);
                }else{
                    ai.boostAction(true);
                    ai.moveAction(Core.camera.position, 12f, false);
                }
            }
        }
    }

    public static class LogicMode extends Mode{
        public static final Seq<Class<? extends LInstruction>> bannedInstructions = new Seq<>();
        public static LogicAI logicAI = new LogicAI();
        public static PopupTable chooseContentTable = new PopupTable(){
            @Override
            public void act(float delta){
                super.act(delta);
                this.keepInScreen();
            }
        };
        static short timerUpdMovement = 0, timerMove = 1, timerShoot = 2, timerTransItemPayload = 3;

        public String code = "";
        public int instructionsPerTick = 10;

        transient public LExecutor exec = new LExecutor();
        transient public boolean itemTrans, payloadTrans;
        transient public StringBuffer log = new StringBuffer();
        transient Queue<BuildPlan> plans = new Queue<>();
        transient public PopupTable customAIUITable = new PopupTable(), logTable = new PopupTable();

        static{
            bannedInstructions.addAll(WriteI.class, ControlI.class, StopI.class, SetBlockI.class, SpawnUnitI.class, ApplyEffectI.class, SetWeatherI.class, SpawnWaveI.class, SetRuleI.class, ExplosionI.class, SetRateI.class, SyncI.class, SetFlagI.class, SetPropI.class, LocalePrintI.class);
        }

        public LogicMode(){
            readCode("");
        }

        @Override
        public void buildConfig(Table table){
            table.table(t -> {
                t.name = "cfg";
                t.defaults().minSize(32f).pad(2f).fill();

                t.button("" + Iconc.terminal + Iconc.blockWorldProcessor, textb, () -> {
                    Runnable shower = () -> {
                        ui.logic.show(code, exec, true, this::readCode);
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
                }).with(funcSetTextb);

                t.button(Core.bundle.get("ai.config.logic.ui") + Iconc.list, textb, () -> {
                    if(customAIUITable.shown){
                        customAIUITable.hide();
                    }else{
                        customAIUITable.popup();
                        customAIUITable.setPositionInScreen(Core.input.mouseX(), Core.input.mouseY());
                    }
                }).with(funcSetTextb);

                t.button("Print" + Iconc.blockWorldMessage, textb, () -> {
                    if(logTable.shown){
                        logTable.hide();
                    }else{
                        logTable.clearChildren();
                        logTable.touchable = Touchable.enabled;
                        logTable.background(Styles.black3);
                        logTable.addCloseButton(40f);
                        logTable.add("Print" + Iconc.blockWorldMessage).size(200f, 40f).with(l -> logTable.addDragPopupListener(l)).row();
                        logTable.image().growX().height(2f).color(Pal.logicOperations).row();
                        logTable.pane(p -> {
                            p.labelWrap(() -> log).growX();
                        }).growX().maxHeight(200f).update(p -> {
                            Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                            if(e != null && e.isDescendantOf(p)) {
                                p.requestScroll();
                            }else if(p.hasScroll()){
                                Core.scene.setScrollFocus(null);
                            }
                        });
                        logTable.popup();
                        logTable.setPositionInScreen(Core.input.mouseX(), Core.input.mouseY());
                    }
                }).with(funcSetTextb);

                t.table(tt -> {
                    tt.defaults().pad(2f).minWidth(32f);
                    tt.add(timerIconStack(() -> "" + Iconc.move, () -> !timer.check(timerMove, LogicAI.logicControlTimeout), () -> (LogicAI.logicControlTimeout - timer.getTime(timerMove)) / 60f));

                    tt.add(timerIconStack(() -> "" + Iconc.commandAttack, () -> !timer.check(timerShoot, LogicAI.logicControlTimeout), () -> (LogicAI.logicControlTimeout - timer.getTime(timerShoot)) / 60f));
                });
            }).grow();
        }

        public void readCode(String str){
            code = str;
            LAssembler asm = LAssembler.assemble(str, true);
            exec.load(asm);
            asm.putConst("@links", exec.links.length);
            asm.putConst("@ipt", instructionsPerTick);
            exec.privileged = true;
            resetCustomUI();
        }

        @Override
        public void act(){
            if(exec.instructions.length == 0){
                if(code != null && !code.isEmpty()) readCode(code); //for loading init
                if(exec.instructions.length == 0) return;
            }
            exec.ipt.numval = Mathf.clamp(instructionsPerTick, 1, ((LogicBlock)Blocks.worldProcessor).maxInstructionsPerTick);
            exec.unit.constant = false;
            var ctrl = ai.unit.controller();
            ai.unit.controller(logicAI);

            //TODO 可调运输冷却
            if(timer.get(timerTransItemPayload, 1)){
                itemTrans = true;
                payloadTrans = true;
            }

            if(timer.get(timerUpdMovement, 5)){
                logicAI.targetTimer = 0f;
                logicAI.controlTimer = LogicAI.logicControlTimeout;
                logicAI.updateMovement();
            }

            plans.clear();
            if(ai.unit.plans != null) ai.unit.plans.each(bp -> plans.add(bp));
            for(int i = 0; i < Mathf.clamp(instructionsPerTick, 1, 2000); i++){
                exec.counter.setnum(Mathf.mod(exec.counter.numi(), exec.instructions.length));
                if(exec.instructions.length == 0) break;
                exec.unit.setobj(ai.unit);
                if(tryRunOverwrite(exec.instructions[exec.counter.numi()])){
                    exec.counter.numval++;
                    continue;
                }
                if(!isLocalSandbox() && bannedInstructions.contains(exec.instructions[exec.counter.numi()].getClass())){
                    exec.counter.numval++;
                    continue;
                }
                exec.runOnce();
            }
            if(ai.unit.plans != null && ai.unit.plans.isEmpty()) plans.each(bp -> ai.unit.plans.add(bp));

            ai.unit.controller(ctrl);
            fullAI.unit(player.unit());

            if(!timer.check(timerMove, LogicAI.logicControlTimeout)){
                ai.boostAction(logicAI.boost);
                if((logicAI.control != LUnitControl.pathfind && logicAI.control != LUnitControl.autoPathfind) || ai.unit.isFlying()){
                    ai.moveAction(logicAI.moveX, logicAI.moveY, logicAI.control == LUnitControl.move ? 1f : Math.max(logicAI.moveRad, 1f), false);
                }else{
                    if(logicAI.control == LUnitControl.pathfind){
                        controlPath.getPathPosition(ai.unit, MI2UTmp.v1.set(logicAI.moveX, logicAI.moveY), MI2UTmp.v2, null);
                        ai.moveAction(MI2UTmp.v2, 0.0005f, false);
                    }

                    if(logicAI.control == LUnitControl.autoPathfind){

                    }
                }
            }

            if(!timer.check(timerShoot, LogicAI.logicControlTimeout)){
                var tgt = logicAI.target(0, 0, 0, false, false);
                if(tgt != null) ai.shootAction(MI2UTmp.v3.set(tgt.getX(), tgt.getY()), logicAI.shoot);
            }
        }

        public boolean isLocalSandbox(){
            return !net.client() || state.rules.mode() == Gamemode.sandbox;
        }

        public boolean tryRunOverwrite(LInstruction inst){
            if(inst instanceof SetRateI sr){
                instructionsPerTick = sr.amount.numi();
                return true;

            }else if(inst instanceof SetPropI li){
                //player set team
                if(li.type.obj() == LAccess.team && li.of.obj() == ai.unit){
                    if(li.value.team() != null) player.team(li.value.team());
                }
                //unit set team
                return false;

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
                        return false;//在这里更新timer，随后执行原版指令来设置ai状态，下同
                    }
                    case move, approach, pathfind, autoPathfind, boost -> {
                        timer.get(timerMove, 1);
                        return false;
                    }
                    case idle -> {
                        timer.reset(timerMove, LogicAI.logicControlTimeout);
                        return true;
                    }
                    case stop -> {
                        timer.reset(timerShoot, LogicAI.logicControlTimeout);
                        return true;
                    }
                    case itemTake -> {
                        if(!(li.p2.obj() instanceof Item item)) return false;
                        if(!itemTrans || player.unit() == null || !player.unit().acceptsItem(item)) return false;
                        Building build = li.p1.building();
                        if(build != null && build.team == logicAI.unit().team && build.isValid() && build.items != null && ai.unit.within(build, itemTransferRange + build.block.size * tilesize/2f)){
                            Call.requestItem(player, build, item, li.p3.numi());
                            itemTrans = false;
                        }
                        return true;
                    }
                    case itemDrop -> {
                        if(!itemTrans || player.unit() == null || player.unit().stack.amount == 0) return false;
                        Building build = li.p1.building();
                        if(build != null && ai.unit.within(build, itemTransferRange + build.block.size * tilesize/2f)){
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
                        Building build = world.buildWorld(ai.unit.x, ai.unit.y);
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

            }else if(inst instanceof ReadI readI){
                //读取其他世处AI的变量
                if(readI.target.isobj && readI.target.objval instanceof CharSequence str){
                    LogicMode lm = null;
                    for(int i = 0; i < ai.modes.size; i++){
                        if(ai.modes.get(i) instanceof LogicMode && ai.modes.get(i).name().contentEquals(str)){
                            lm = (LogicMode)ai.modes.get(i);
                        }
                    }
                    if(lm != null && readI.position.isobj && readI.position.objval instanceof String name){
                        LVar fromVar = lm.exec.optionalVar(name);
                        if(fromVar != null && !readI.output.constant){
                            readI.output.objval = fromVar.objval;
                            readI.output.numval = fromVar.numval;
                            readI.output.isobj = fromVar.isobj;
                            return true;
                        }
                    }
                }
                return false;

            }else if(inst instanceof WriteI writeI){
                //写入其他世处AI的变量
                if(writeI.target.isobj && writeI.target.objval instanceof CharSequence str){
                    LogicMode lm = null;
                    for(int i = ai.modes.size - 1; i >= 0; i--){
                        if(ai.modes.get(i) instanceof LogicMode && ai.modes.get(i).name().contentEquals(str)){
                            lm = (LogicMode)ai.modes.get(i);
                        }
                    }
                    if(lm != null && writeI.position.isobj && writeI.position.objval instanceof String name){
                        LVar toVar = lm.exec.optionalVar(name);
                        if(toVar != null && !toVar.constant){
                            toVar.objval = writeI.value.objval;
                            toVar.numval = writeI.value.numval;
                            toVar.isobj = writeI.value.isobj;
                            return true;
                        }
                    }
                }
                return false;
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
                resetCustomUI();
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
                        b.clicked(() -> b.cullable = !b.cullable);  //作为是否启用追踪视角的toggler
                        b.update(() -> {
                            b.setText(target.obj() != null ? PrintI.toString(target.obj()) : Strings.autoFixed(target.numfOrNan(), 3));
                            b.setColor(!b.cullable ? Color.cyan : Color.white);
                            if(b.hasMouse()) HoverTopTable.hoverInfo.setHovered(target.obj());
                            if(!b.cullable && (target.obj() instanceof Posc posc)) InputUtils.panStable(posc);
                        });
                    });
                }
            }

            return true;
        }

        public void resetCustomUI(){
            customAIUITable.clear();
            customAIUITable.touchable = Touchable.enabled;
            customAIUITable.margin(2f);
            customAIUITable.background(Styles.black3);
            customAIUITable.addCloseButton(40f);
            customAIUITable.add("@ai.config.logic.ui").height(40f).growX().with(l -> customAIUITable.addDragPopupListener(l)).row();
            customAIUITable.image().colspan(4).growX().height(2f).color(Pal.logicWorld).row();
            customAIUITable.update(() -> customAIUITable.keepInScreen());
            customAIUITable.defaults().size(100, 32);
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
    }
}