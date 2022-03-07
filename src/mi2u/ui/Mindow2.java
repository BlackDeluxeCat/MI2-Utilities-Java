package mi2u.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.Label.*;
import arc.scene.ui.TextField.TextFieldValidator;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.io.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mi2u.MI2UVars.*;
/**  
 * Mindow2 is a dragable Table that partly works like a window. 
 * @param titleText is text shown on titleBar, set in constructor.
 * @param mindowName is inner name, used for window-specific settings, set in overrided constructor.
 * @param helpInfo is text shown in window-specific help dialog, set in constructor.
 * @param cont is a container for user items. 
 * @param settings is a SettingEntry seq.
 * <p>
 * {@code setupCont(Table cont)}for cont rebuild, should be overrided.<p>
 * {@code initSettings()}for customize settings, should start with settings.clear()
 * @author BlackDeluxeCat
 */

public class Mindow2 extends Table{
    @Nullable public static Mindow2 currTopmost = null;
    public static LabelStyle titleStyleNormal, titleStyleSnapped;
    public static Drawable titleBarbgNormal, titleBarbgSnapped;

    public float fromx = 0, fromy = 0, curx = 0, cury = 0, titleScale = 1f;
    public boolean topmost = false, minimized = false, closable = true;
    public String titleText = "", helpInfo = "", mindowName;
    protected Table titleBar = new Table();
    protected Table cont = new Table();
    protected Seq<SettingEntry> settings = new Seq<>();
    protected Interval interval = new Interval(2);
    @Nullable public Element aboveSnap; public int edgesnap = -1;

    public Mindow2(String title){
        titleText = title;
        rebuild();
    }

    public Mindow2(String title, String help){
        this(title);
        helpInfo = help;
    }

    public void rebuild(){
        clear();
        initSettings();
        setupTitle();
        row();
        if(!minimized){
            cont.setBackground(Styles.black3);
            add(cont);
            setupCont(cont);
        }
    }

    /** called when rebuild Mindow2, should be overrided */
    public void setupCont(Table cont){}

    /** called when click minimize-button, can be overrided */
    public void minimize(){
        rebuild();
    }

    /** called when click close-button, can be overrided */
    public void close(){
        remove();
    }

    public void setupTitle(){
        titleBar.clear();
        var title = new Label(titleText);
        title.name = "Mindow2Title";
        title.setAlignment(Align.left);
        title.addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                fromx = x;
                fromy = y;
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                Vec2 v = localToStageCoordinates(Tmp.v1.set(x, y));
                Element hit = Core.scene.hit(v.x + title.x + title.parent.x, v.y + title.y + title.parent.y, false);
                if(hit != null && hit.name == "Mindow2Title" && !hit.isDescendantOf(Mindow2.this)){
                    aboveSnap = hit.parent.parent;
                    return;
                }
                aboveSnap = null;
                curx = v.x - fromx;
                cury = v.y - fromy;
            }
        });
        titleBar.add(title).pad(0, 1, 0, 1).growX();

        titleBar.button("" + Iconc.info, textb, () -> {
            showHelp();
        }).size(titleButtonSize);

        titleBar.button("" + Iconc.settings, textb, () -> {
            showSettings();
        }).size(titleButtonSize);

        titleBar.button("" + Iconc.lock, textbtoggle, () -> {
            topmost = !topmost;
            if(topmost){
                currTopmost = this;
            }else{
                if(currTopmost == this) currTopmost = null;
            }
            rebuild();
        }).size(titleButtonSize).update(b -> {
            topmost = currTopmost == this;
            b.setChecked(topmost);
        });

        titleBar.button("-", textbtoggle, () -> {
            minimized = !minimized;
            if(minimized){
                cury += cont.getHeight();
            }else{
                cury -= cont.getHeight();
            }
            minimize();
        }).size(titleButtonSize).update(b -> {
            b.setChecked(minimized);
        });

        titleBar.button("X", textb, () -> {
            close();
        }).size(titleButtonSize).update(b -> {
            b.setDisabled(!closable);
        });

        addListener(new InputListener(){
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
                titleScale = 1f;
                interval.get(1, 1);
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
                titleScale = 0f;
                interval.get(0, 1);
            }
        });

        titleBar.setTransform(true);
        //titleBar.setClip(true);
        interval.reset(0, 1);
        interval.reset(1, 1);
        titleBar.update(() -> {
            cont.touchable = Touchable.enabled;
            title.setStyle(aboveSnap == null ? titleStyleNormal : titleStyleSnapped);
            titleBar.setBackground(aboveSnap == null ? titleBarbgNormal : titleBarbgSnapped);
            //don't collapse when minimized
            if(minimized || !MI2USettings.getBool(mindowName + ".autoHideTitle", false)) titleScale = 1f;
            if((interval.check(0, 180) && titleBar.scaleY > 0.95f) || (interval.check(1, 15) && titleBar.scaleY < 0.95f)){
                titleBar.toFront();
                titleBar.setScale(1, Mathf.lerpDelta(titleBar.scaleY, titleScale, 0.3f));
                titleBar.keepInStage();
                titleBar.invalidateHierarchy();
                titleBar.pack();
                titleBar.touchable = Touchable.enabled;
            }else if(titleBar.scaleY < 0.95f){
                titleBar.touchable = Touchable.disabled;
            }
            edgesnap = MI2USettings.getInt(mindowName + ".edgesnap", -1);
            if(this == currTopmost || shouldTopMost()) setZIndex(1000);

            if(aboveSnap != null){
                curx = aboveSnap.x;
                cury = aboveSnap.y - getRealHeight();
                setPosition(curx, cury);
                keepInStage();
            }else if(edgesnap != -1 && hasParent()){
                edgeSnap(edgesnap);
                setPosition(curx, cury);
            }else{
                curx = Mathf.clamp(curx, 0, (hasParent() ? parent.getWidth() : Core.graphics.getWidth()) - getWidth());
                cury = Mathf.clamp(cury, 0, (hasParent() ? parent.getHeight() : Core.graphics.getHeight()) -getRealHeight());
                setPosition(curx, cury);
                keepInStage();
            }
            invalidateHierarchy();
            pack();
        });
        add(titleBar).growX();
    }

    protected void edgeSnap(int s){
        switch(s){
            case 0://lefttop
                curx = 0;
                cury = parent.getHeight() -getRealHeight();
                break;
            case 1://top
                curx = (parent.getWidth() - getWidth())/2f;
                cury = (parent.getHeight() -getRealHeight());
                break;
            case 2://righttop
                curx = (parent.getWidth() - getWidth());
                cury = (parent.getHeight() -getRealHeight());
                break;
            case 3://right
                curx = (parent.getWidth() - getWidth());
                cury = (parent.getHeight() -getRealHeight())/2f;
                break;
            case 4://rightbottom
                curx = (parent.getWidth() - getWidth());
                cury = 0;
                break;
            case 5://bottom
                curx = (parent.getWidth() - getWidth())/2f;
                cury = 0;
                break;
            case 6://leftbottom
                curx = 0;
                cury = 0;
                break;
            case 7://left
                curx = 0;
                cury = (parent.getHeight() -getRealHeight())/2f;
                break;
        }
    }

    public boolean addTo(Group newParent){
        if(newParent == null){
            return !this.remove();
        }
        this.remove();
        newParent.addChild(this);
        return true;
    }

    public float getRealHeight(){
        return getHeight() - titleBar.getHeight() *  (1 - titleBar.scaleY);
    }

    public boolean shouldTopMost(){
        return (topmost || (aboveSnap !=null && aboveSnap != this && aboveSnap instanceof Mindow2 m && m.shouldTopMost()));
    }
    
    public void showHelp(){
        new BaseDialog("@mindow2.helpInfoTitle"){
            {
                addCloseButton();
                this.cont.pane(t -> {
                    t.add(helpInfo).padBottom(60f).left().width(Core.graphics.getWidth() / 1.5f).get().setWrap(true);
                    t.row();
                    t.add("@mindow2.uiHelp").left().width(Core.graphics.getWidth() / 1.5f).get().setWrap(true);
                });
                show();
            }
        };
    }

    /** Settings shoulded be set in Seq: settings, will be shown and configurable in SettingsDialog 
     * UISetting will be shown to, but not configurable
    */
    public void showSettings(){
        new BaseDialog("@mindow2.settings.title"){
            {
                addCloseButton();
                this.cont.pane(t -> {
                    t.add(mindowName != null && !mindowName.equals("") ? Core.bundle.format("mindow2.settings.curMindowName") + mindowName: "@mindow2.settings.noMindowNameWarning").fontScale(1.2f).get().setAlignment(Align.center);
                    t.row();
                    settings.each(st -> {
                        t.add(st.getF()).width(Math.min(500, Core.graphics.getWidth())).left();
                        t.row();
                    });
                    t.table(tt -> {
                        tt.button("@mindow2.settings.reloadUI", textb, () -> {
                            loadUISettings();
                        }).width(100).get().getLabel().setColor(1, 1, 0, 1);
    
                        tt.button("@mindow2.settings.cacheUI", textb, () -> {
                            saveUISettings();
                        }).width(100).get().getLabel().setColor(1, 1, 0, 1);
    
                        tt.button("@mindow2.settings.writeToFile", textb, () -> {
                            MI2USettings.save();
                        }).width(100);
                    }).self(c -> {
                        c.width(Math.min(530, Core.graphics.getWidth()));
                    });

                });
                show();
            }
        };
    }

    /** can be overrided, should use super.initSettings(), called in rebuild() */
    public void initSettings(){
        settings.clear();
        if(mindowName == null || mindowName.equals("")) return;
        settings.add(new SettingEntry(mindowName + ".minimized", true));
        settings.add(new SettingEntry(mindowName + ".topmost", true));
        settings.add(new SettingEntry(mindowName + ".curx", true));
        settings.add(new SettingEntry(mindowName + ".cury", true));
        var f = new FieldSettingEntry(SettingType.Int, mindowName + ".edgesnap", s -> {
            return Strings.canParseInt(s) && Strings.parseInt(s) <= 7 && Strings.parseInt(s) >= -1;
        }, "@settings.mindow.edgesnap");
        f.isUI = true;
        settings.add(f);
        f = new FieldSettingEntry(SettingType.Str, mindowName + ".abovesnapTarget", s -> {
            return mindow2s.contains(mi2 -> mi2.mindowName.equals(s)) || s.equals("null");
        }, "@settings.mindow.abovesnapTarget");
        f.isUI = true;
        settings.add(f);
        settings.add(new CheckSettingEntry(mindowName + ".autoHideTitle", "@settings.mindow.autoHideTitle"));
    }

    /** Override this method for custom UI settings load
     * rebuild() called once finished loading
     */
    public boolean loadUISettingsRaw(){
        //it is a not-named mindow2, no settings can be loaded.
        if(mindowName == null || mindowName.equals("")) return false;
        minimized = MI2USettings.getBool(mindowName + ".minimized");
        topmost = MI2USettings.getBool(mindowName + ".topmost");
        if(topmost) currTopmost = this;
        edgesnap = MI2USettings.getInt(mindowName + ".edgesnap", -1);
        curx = (float)MI2USettings.getInt(mindowName + ".curx");
        cury = (float)MI2USettings.getInt(mindowName + ".cury");
        if(MI2USettings.getStr(mindowName + ".abovesnapTarget").equals("null")){
            aboveSnap = null;
        }else{
            mindow2s.each(m -> {
                if(m.mindowName.equals(MI2USettings.getStr(mindowName + ".abovesnapTarget"))){
                    aboveSnap = m;
                    Log.info(mindowName + " snaps to " + m.mindowName);
                }
            });
        }
        return true;
    }

    public void loadUISettings(){
        loadUISettingsRaw();
        rebuild();
    }

    /** Override this method for custom UI settings save
     */
    public boolean saveUISettings(){
        //it is a not-named mindow2, no settings can be saved.
        if(mindowName == null || mindowName.equals("")) return false;
        MI2USettings.putBool(mindowName + ".minimized", minimized);
        MI2USettings.putBool(mindowName + ".topmost", topmost);
        MI2USettings.putInt(mindowName + ".edgesnap", edgesnap);
        //edgesnap will change curx cury, so xy shouldn't be saved when edgesnapping.
        if(edgesnap == -1){
            MI2USettings.putInt(mindowName + ".curx", (int)curx);
            MI2USettings.putInt(mindowName + ".cury", (int)cury);
        }
        return true;
    }

    public boolean registerName(){
        if(mindowName != null && !mindowName.equals("") && !mindow2s.contains(m -> m.mindowName.equals(this.mindowName))){
            mindow2s.add(this);
            return true;
        }
        return false;
    }

    public static void init(){
        var whiteui = (TextureRegionDrawable)Tex.whiteui;
        titleStyleNormal = new LabelStyle(Fonts.def, new Color(0.8f,0.9f,1f,1f));
        //titleStyleNormal.background = whiteui.tint(1f, 0.1f, 0.2f, 0.8f);
        titleStyleSnapped = new LabelStyle(Fonts.def, new Color(0.1f,0.6f,0.6f,1f));
        //titleStyleSnapped.background = whiteui.tint(1f, 0.1f, 0.2f, 0.2f);
        titleBarbgNormal = whiteui.tint(1f, 0.1f, 0.2f, 0.8f);
        titleBarbgSnapped = whiteui.tint(1f, 0.1f, 0.2f, 0.2f);
    }

    public class SettingEntry{
        public String name = "";
        public boolean isUI = false;
        protected SettingType type;
        protected Table cont;
        public SettingEntry(String name){
            this.name = name;
            cont = new Table();
            cont.label(() -> this.name + " = " + MI2USettings.getStr(this.name)).update(t -> {
                t.setColor(1,1,this.isUI?0:1,1);
            });
        }
        public SettingEntry(String name, boolean isUI){
            this(name);
            this.isUI = isUI;
        }
        public Table getF(){
            return cont;
        }
    }

    public class CheckSettingEntry extends SettingEntry{
        Boolc changed;
        public CheckSettingEntry(String name, String help, Boolc changed){
            super(name);
            this.changed = changed;
            cont.clear();
            TextButton tb = new TextButton(this.name, textbtoggle);
            cont.add(tb).left().with(c -> {
                c.getLabelCell().width(200).height(32);
                c.getLabel().setWrap(true);
                c.getLabel().setAlignment(Align.left);
            });
            
            tb.changed(() -> {
                MI2USettings.putBool(this.name, tb.isChecked());
                if(this.changed != null){
                    changed.get(tb.isChecked());
                }
            });
            tb.update(() -> {
                tb.setChecked(MI2USettings.getBool(this.name));
            });
            cont.add(help).right().self(c -> {
                c.growX();
                c.get().setWrap(true);
                c.get().setAlignment(Align.right);
                c.get().setColor(1, 1, 1, 0.7f);
            }).update(t -> {
                t.setColor(1,1,this.isUI?0:1,1);
            });
        }

        public CheckSettingEntry(String name, String help){
            this(name, help, null);
        }
    }

    public class FieldSettingEntry extends SettingEntry{
        Cons<String> changed;
        /** be careful that {@code Cons<String> changed} input a string */
        public FieldSettingEntry(SettingType ty, String name, TextFieldValidator val , String help, Cons<String> changed){
            super(name);
            this.changed = changed;
            cont.clear();
            type = ty;
            TextField tf = new TextField("", Styles.nodeField);
            tf.setValidator(val);
            tf.changed(() -> {
                if(val == null || val.valid(tf.getText())){
                    switch(type){
                        case Bool:
                            MI2USettings.putBool(this.name, tf.getText().equals("true") ? true:false);
                            break;
                        case Int:
                            MI2USettings.putInt(this.name, Strings.parseInt(tf.getText(), 0));
                            break;
                        default:
                            MI2USettings.putStr(this.name, tf.getText() != null ? tf.getText():"");
                            break;
                    }
                    if(this.changed != null){
                        changed.get(MI2USettings.getStr(this.name));
                    }
                }
            });
            cont.table(tt -> {
                tt.add(tf).width(200f).left().padRight(20);
                tt.row();
                tt.label(() -> this.name + " = " + MI2USettings.getStr(this.name)).get().setColor(0, 1, 1, 0.7f);
            });
            cont.add(help).right().self(c -> {
                c.growX();
                c.get().setWrap(true);
                c.get().setAlignment(Align.right);
                c.get().setColor(1, 1, 1, 0.7f);
            }).update(t -> {
                t.setColor(1,1,this.isUI?0:1,1);
            });
        }

        public FieldSettingEntry(SettingType ty, String name, TextFieldValidator val , String help){
            this(ty, name, val , help, null);
        }
    }

    public static enum SettingType{
        Int, Str, Bool
    }
}
