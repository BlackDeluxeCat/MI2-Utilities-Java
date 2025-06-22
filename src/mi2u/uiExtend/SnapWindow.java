package mi2u.uiExtend;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.struct.FloatSeq;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import arc.util.Tmp;
import mi2u.MI2UTmp;
import mi2u.MI2UVars;
import mi2u.io.SettingHandler;
import mi2u.ui.elements.Mindow2;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import static arc.Core.*;
import static arc.Core.camera;
import static mi2u.MI2UVars.*;

public class SnapWindow extends SnapElement {
    public Table cont = new Table();
    public SettingHandler settings;

    public SnapWindow(String name){
        this.name = name;
        registerToGlobal();
        initSettings();
        loadUISettings();

        Events.on(EventType.ResizeEvent.class, e -> Time.run(60f, () -> {
            loadUISettings();
            rebuild();
        }));

        Events.on(EventType.ClientLoadEvent.class, e -> rebuild());

        cont.setBackground(Styles.black3);
    }

    public void rebuild(){
        clear();

        cont.touchable = Touchable.enabled;
        setupCont(cont);
        Table overlay = new Table();
        //todo
        overlay.table(t -> {
            t.background(Styles.black3);
            t.center().image(Icon.move).size(32f).row();
            t.label(() -> (int)snapX + "," + (int)snapY);
        }).grow().visible(() -> dragging).touchable(() -> dragging? Touchable.enabled : Touchable.disabled);
        overlay.addListener(new InputListener(){
            final Vec2 mousePos = new Vec2();
            final Seq<Element> snapGroup = new Seq<>();
            final FloatSeq elementDst = new FloatSeq();
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                snapGroup.clear();
                elementDst.clear();

                mousePos.set(x, y);
                if (button == KeyCode.mouseLeft){
                    snapGroup.add(SnapWindow.this);
                    elementDst.add(0, 0);
                    return true;
                }
                if (button == KeyCode.mouseMiddle){
                    getSnappedElements(snapGroup);
                    for (Element e : snapGroup){
                        elementDst.add(e.x - SnapWindow.this.x, e.y - SnapWindow.this.y);
                    }
                    return true;
                }
                if (button == KeyCode.mouseRight){
                    showSettings();
                    return false;
                }
                return false;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                Vec2 v = localToStageCoordinates(MI2UTmp.v1.set(x, y)).sub(mousePos);
                for (int i = 0; i < snapGroup.size; i++){
                    Element e = snapGroup.get(i);
                    float w = elementDst.get(i * 2);
                    float h = elementDst.get(i * 2 + 1);

                    if (e instanceof SnapElement snapElement){
                        snapElement.snapX = v.x + w;
                        snapElement.snapY = v.y + h;
                    }

                    e.x = v.x + w;
                    e.y = v.y + h;
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                super.touchUp(event, x, y, pointer, button);
                for (Element e : snapGroup) {
                    if (e instanceof SnapWindow snapWindow){
                        snapWindow.setSnap();
                        snapWindow.saveUISettings();
                    }
                }
            }
        });

        Stack stack = new Stack(cont, overlay);
        stack.update(this::updateDragging);

        add(stack).growX();

        setTransform(true);
    }

    /**
     * called when rebuild Mindow2, should be overridden
     */
    public void setupCont(Table cont){}

    @Override
    public void act(float delta){
        super.act(delta);
        if (!dragging){
            if (snapX >= 0f) x = Mathf.lerp(x, snapX, 0.3f);
            if (snapY >= 0f) y = Mathf.lerp(y, snapY, 0.3f);
        }

        keepInStage();
        invalidate();
        pack();
    }

    public void setSnap(){
        Element minimap = Vars.ui.hudGroup.find("minimap");
        outerElementSnap(minimap);
        //todo use quadtree
        mindow2s.each(this::outerElementSnap);
        windows.each(this::outerElementSnap);
        innerElementSnap(parent);
    }

    public void addTo(Group newParent){
        if(newParent == null){
            this.remove();
            return;
        }
        newParent.addChild(this);
    }

    /// Settings should be set in Seq: settings, will be shown and configurable in SettingsDialog
    public void showSettings(){
        new BaseDialog("@settings.meta.dialogTitle"){
            {
                addCloseButton();
                this.cont.pane(t -> {
                    t.button("@settings.meta.mindowHelp", Icon.info, () -> new BaseDialog(""){
                        {
                            addCloseButton();
                            this.cont.pane(t1 -> t1.add("@" + SnapWindow.this.name + ".help").padBottom(60f).left().width(graphics.getWidth() / 1.5f).get().setWrap(true));
                            show();
                        }
                    }).disabled(!bundle.has(SnapWindow.this.name + ".help")).width(300f).height(150f).get().setStyle(textb);
                    t.row();
                    t.table(tt -> settings.buildList(tt)).width(Math.min(600, Core.graphics.getWidth()));
                }).grow().row();
                this.cont.addChild(new Table(){
                    {
                        touchable = Touchable.disabled;
                        update(() -> {
                            cont.stageToLocalCoordinates(MI2UTmp.v3.set(Core.input.mouse()));
                            this.setPosition(MI2UTmp.v3.x, MI2UTmp.v3.y + 100);
                        });
                    }
                });
                show();
            }
        };
    }

    /// can be overridden, should use super.initSettings(), called in rebuild()
    public void initSettings(){
        if(name == null || name.isEmpty()) return;
        if(settings == null) settings = new SettingHandler(name);
        settings.list.clear();
        var sScl = settings.sliderPref("scale", 100, 20, 400, 10, s -> s + "%", scl -> setScale(scl / 100f));
        sScl.title = bundle.get("settings.mindow.scale");
        sScl.description = bundle.getOrNull("settings.mindow.scale.description");
    }

    public void loadUISettings(){
        if(name == null || name.isEmpty()) return;
        setScale(settings.getInt("scale", 100) / 100f);
        x = settings.getInt("tableX", (int) (camera.width/2f));
        y = settings.getInt("tableY", (int) (camera.height/2f));
    }

    /// Override this method for custom UI settings save
    public void saveUISettings(){
        if(name == null || name.isEmpty()) return;
        settings.putInt("scale", (int)(scaleX * 100));
        settings.putInt("tableX", (int) snapX);
        settings.putInt("tableY", (int) snapY);
    }

    public void registerToGlobal(){
        if(name != null && !name.isEmpty() && !mindow2s.contains(m -> m.name.equals(this.name))){
            windows.add(this);
        }
    }
}
