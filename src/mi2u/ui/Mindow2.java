package mi2u.ui;

import arc.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.Label.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.io.MI2USettings;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mi2u.MI2UVars.*;
/**  
 * Mindow2 is a dragable Table that partly works like a window. 
 * cont is a container for user items. 
 * setupCont() should be overrided, which will be called when rebuild().
 * @author BlackDeluxeCat
 */

public class Mindow2 extends Table{
    @Nullable public static Mindow2 currTopmost = null;
    public static LabelStyle titleStyleNormal, titleStyleSnapped;
    public static Drawable titleBarbgNormal, titleBarbgSnapped;

    public float fromx = 0, fromy = 0, curx = 0, cury = 0;
    public boolean topmost = false, minimized = false, closable = true;
    public String titleText = "", helpInfo = "", mindowName;
    private Table titleBar = new Table();
    private Table cont = new Table();
    @Nullable public Element aboveSnap;

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
        setupTitle();
        row();
        if(!minimized){
            cont.setBackground(Styles.black3);
            add(cont);
            setupCont(cont);
        }
    }

    /** called when rebuild Mindow2, should be overrided */
    public void setupCont(Table cont){

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

        titleBar.button("" + Iconc.save, textb, () -> {
            saveSettings();
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
            rebuild();
        }).size(titleButtonSize).update(b -> {
            b.setChecked(minimized);
        });

        titleBar.button("X", textb, () -> {
            remove();
        }).size(titleButtonSize).update(b -> {
            b.setDisabled(!closable);
        });

        add(titleBar).update(tb -> {
            curx = Mathf.clamp(curx, 0, (hasParent() ? parent.getWidth() : Core.graphics.getWidth()) - getWidth());
            cury = Mathf.clamp(cury, 0, (hasParent() ? parent.getHeight() : Core.graphics.getHeight()) - getHeight());
            if(aboveSnap != null){
                curx = aboveSnap.x;
                cury = aboveSnap.y - getHeight();
            }
            setPosition(curx, cury);
            keepInStage();
            invalidateHierarchy();
            pack();
            if(this == currTopmost || shouldTopMost()) setZIndex(1000);
            title.setStyle(aboveSnap == null ? titleStyleNormal : titleStyleSnapped);
            titleBar.setBackground(aboveSnap == null ? titleBarbgNormal : titleBarbgSnapped);
        }).growX().fillY();
    }

    public boolean addTo(Group newParent){
        if(newParent == null){
            return !this.remove();
        }
        this.remove();
        newParent.addChild(this);
        return true;
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

    /** Override this method for custom settings load
     * rebuild() called once finished loading
     */
    public boolean loadSettingsRaw(){
        //it is a not-named mindow2, no settings can be loaded.
        if(mindowName == null || mindowName.equals("")) return false;
        minimized = MI2USettings.getBool(mindowName + ".minimized");
        topmost = MI2USettings.getBool(mindowName + ".topmost");
        if(topmost) currTopmost = this;
        curx = (float)MI2USettings.getInt(mindowName + ".curx");
        cury = (float)MI2USettings.getInt(mindowName + ".cury");
        return true;
    }

    public void loadSettings(){
        loadSettingsRaw();
        rebuild();
    }

    /** Override this method for custom settings load
     */
    public boolean saveSettings(){
        //it is a not-named mindow2, no settings can be saved.
        if(mindowName == null || mindowName.equals("")) return false;
        MI2USettings.putBool(mindowName + ".minimized", minimized);
        MI2USettings.putBool(mindowName + ".topmost", topmost);
        MI2USettings.putInt(mindowName + ".curx", (int)curx);
        MI2USettings.putInt(mindowName + ".cury", (int)cury);
        return true;
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
}
