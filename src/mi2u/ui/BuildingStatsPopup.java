package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.struct.*;
import arc.util.*;
import mi2u.*;
import mi2u.input.*;
import mi2u.ui.elements.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.ui.*;
import mindustry.world.blocks.logic.*;

import static mi2u.MI2UVars.*;
import static mindustry.Vars.*;

public class BuildingStatsPopup<B extends Building> extends PopupTable{
    public static ObjectMap<Object, Cons> popNew = new ObjectMap<>();

    public static void popNew(Building b){
        if(b == null) return;
        var cons = popNew.get(b.getClass());
        if(cons != null) cons.get(b);
    }

    public static <T> void putPop(Class<T> clazz, Cons<T> cons){
        popNew.put(clazz, cons);
    }

    public static void init(){
        putPop(LogicBlock.LogicBuild.class, LogicBuildPopup::new);
        putPop(MessageBlock.MessageBuild.class, MessageBuildPopup::new);
    }

    public B build;
    public Image imageBlock;
    public Label labelPos;

    public Interval timer;
    public int timerRate = 1;
    public boolean timerUI, pause;

    public BuildingStatsPopup(B build){
        super();
        this.addInGameVisible();
        this.touchable = Touchable.enabled;
        init(build);
        setupPopup();
        addDragMove();
        update(() -> {
            checkValid();
            upd();
            Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
            if(e == null || (!e.isDescendantOf(this) && hasScroll())){
                Core.scene.setScrollFocus(null);
            }
            if(!state.isGame()) hide();
        });
        popup();
    }

    public void init(B build){
        this.build = build;
    }

    public void checkValid(){
        if(build == null || !build.isValid()){
            clear();
            hide();
        }
    }

    public void setupPopup(){
        clearChildren();
        table(t -> {
            t.background(Styles.black8);
            imageBlock = t.image(build.block.uiIcon).size(24f).get();
            imageBlock.clicked(() -> {
                if(control.input instanceof InputOverwrite io) io.pan(true, MI2UTmp.v1.set(build));
                imageBlock.actions(Actions.color(Color.acid), Actions.color(Color.white, 0.2f));
            });
            labelPos = t.label(() -> Iconc.left + Strings.autoFixed(build.x / tilesize, 1) + ", "+ Strings.autoFixed(build.y / tilesize, 1)).get();
        }).growX().left().get();
        if(timer != null && timerUI){
            row();
            table(t -> {
                t.background(Styles.black5);
                t.defaults().width(20f);
                t.button("<<", textb, () -> {
                    timerRate = Math.max(timerRate - 10, 1);
                }).with(funcSetTextb);
                t.button("<", textb, () -> {
                    timerRate = Math.max(timerRate - 1, 1);
                }).with(funcSetTextb);
                t.label(() -> Iconc.settings + String.valueOf(timerRate)).width(60f);
                t.button(">", textb, () -> {
                    timerRate = Math.max(timerRate + 1, 1);
                }).with(funcSetTextb);
                t.button(">>", textb, () -> {
                    timerRate = Math.max(timerRate + 10, 1);
                }).with(funcSetTextb);
                t.button(Iconc.pause + "", textbtoggle, () -> {
                    pause(!pause);
                }).with(funcSetTextb).width(40f).padLeft(10f).checked(b -> pause);
            }).growX();
        }
        addCloseButton();
    }

    public void upd(){
    }

    public boolean check(){
        return timer != null && !pause && timer.check(0, timerRate-1);
    }

    public boolean clock(){
        if(timer != null && !pause) return timer.get(0, timerRate);
        return false;
    }

    public void pause(boolean pause){
        this.pause = pause;
    }

    public static class LogicBuildPopup extends BuildingStatsPopup<LogicBlock.LogicBuild>{
        public int varsHash;

        public LogicBuildPopup(LogicBlock.LogicBuild build){
            super(build);
            varsHash = build.executor.vars.hashCode();
        }

        @Override
        public void init(LogicBlock.LogicBuild build){
            super.init(build);
            timer = new Interval();
            timerRate = 15;
            timerUI = true;
        }

        @Override
        public void setupPopup() {
            super.setupPopup();
            row();
            pane(t -> {
                t.background(Styles.black5);
                t.defaults().left();
                for(var var : build.executor.vars){
                    var label = t.add("").get();
                    label.update(() -> {
                        if(check()){
                            var str = var.name + ": " + (var.isobj ? LExecutor.PrintI.toString(var.objval) : Math.abs(var.numval - (long)var.numval) < 0.00001 ? (long)var.numval + "" : var.numval + "");
                            if(!label.textEquals(str)){
                                label.setText(str);
                                label.clearActions();
                                label.actions(Actions.color(Color.acid), Actions.color(Color.white, 0.1f));
                            }
                        }
                    });

                    t.row();
                }
            }).maxHeight(Core.graphics.getHeight()/2f).width(300f);
        }

        @Override
        public void upd() {
            super.upd();
            int hash = build.executor.vars.hashCode();
            if(varsHash != hash){
                setupPopup();
                varsHash = hash;
                imageBlock.actions(Actions.color(Color.green), Actions.color(Color.white, 0.2f));
            }
            clock();
        }
    }

    public static class MessageBuildPopup extends BuildingStatsPopup<MessageBlock.MessageBuild>{
        public String[] history;
        public int index = 0;

        public MessageBuildPopup(MessageBlock.MessageBuild build){
            super(build);
        }

        @Override
        public void init(MessageBlock.MessageBuild build){
            super.init(build);
            history = new String[10];
            timer = new Interval();
            timerRate = 15;
            timerUI = true;
        }

        @Override
        public void setupPopup() {
            super.setupPopup();
            row();
            pane(t -> {
                t.background(Styles.black5);
                t.defaults().left();
                for(int i = 0; i < history.length; i++){
                    int is = i;
                    t.label(() -> history[Mathf.mod(index - is + history.length, history.length)]);
                    t.row();
                    t.add(new Image(Tex.whiteui, Color.acid)).height(4f).growX();
                    t.row();
                }
            }).maxHeight(Core.graphics.getHeight()/2f).width(300f);
        }

        @Override
        public void upd() {
            super.upd();
            if(clock()){
                var str = build.message == null || build.message.length() == 0 ? "[lightgray]" + Core.bundle.get("empty") : build.message.toString();
                if(history[index] == null || !history[index].equals(str)){
                    index++;
                    if(index >= history.length) index = 0;
                    history[index] = str;
                }
            }
        }
    }
}

