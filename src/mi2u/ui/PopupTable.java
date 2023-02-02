package mi2u.ui;

import arc.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mi2u.*;
import mindustry.*;
import mindustry.gen.*;

public class PopupTable extends Table{
    public boolean shown = false;
    public float fromx, fromy;

    public void popup(int align){
        if(shown) return;
        Core.scene.add(this);
        shown = true;
        setTransform(true);
        clearActions();
        float duration = 0.15f;
        if(Align.isTop(align)){
            actions(Actions.scaleTo(1f,0f), Actions.translateBy(0f, getPrefHeight()));
            actions(Actions.parallel(Actions.scaleTo(1f, 1f, duration, Interp.fade), Actions.translateBy(0f , -getPrefHeight(), duration, Interp.fade), Actions.run(this::keepInScreen)));
        }else if(Align.isBottom(align)){
            actions(Actions.scaleTo(1f,0f));
            actions(Actions.parallel(Actions.scaleTo(1f, 1f, duration, Interp.fade), Actions.run(this::keepInScreen)));
        }else if(Align.isLeft(align)){
            actions(Actions.scaleTo(0f,1f));
            actions(Actions.parallel(Actions.scaleTo(1f, 1f, duration, Interp.fade), Actions.run(this::keepInScreen)));
        }else if(Align.isRight(align)){
            actions(Actions.scaleTo(0f,1f), Actions.translateBy(getPrefWidth(), 0f));
            actions(Actions.parallel(Actions.scaleTo(1f, 1f, duration, Interp.fade), Actions.translateBy(-getPrefWidth() , 0f, duration, Interp.fade), Actions.run(this::keepInScreen)));
        }
    }

    public void popup(){popup(Align.left);}

    public void hide(){
        if(!shown) return;
        shown = false;
        actions(Actions.scaleTo(1f,0f, 0.15f, Interp.fade), Actions.delay(0.15f), Actions.remove());
    }

    public void snapTo(Element e){
        e.localToStageCoordinates(MI2UTmp.v1.setZero());
        setPositionInScreen(MI2UTmp.v1.x, MI2UTmp.v1.y);
    }

    public void setPositionInScreen(float x, float y){
        setPosition(x, y);
        keepInScreen();
    }

    public void keepInScreen(){
        keepInStage();
        invalidateHierarchy();
        pack();
    }

    public void hideWithoutFocusOn(@Nullable Element...elements){
        boolean hasMouse = false;
        if(elements != null){
            for(Element e : elements){
                if(e != null && e.hasMouse()){
                    hasMouse = true;
                    break;
                }
            }
        }
        if(!hasMouse) hide();
    }

    public void addDragMove(){
        addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                fromx = x;
                fromy = y;
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                Vec2 v = localToStageCoordinates(MI2UTmp.v1.set(x, y));
                setPositionInScreen(v.x - fromx, v.y - fromy);
            }
        });
    }

    public void addCloseButton(){
        addCloseButton(32f);
    }

    public void addCloseButton(float size){
        TextButton b = new TextButton("" + Iconc.cancel, MI2UVars.textb){{clicked(() -> hide());}};
        b.setStyle(new TextButton.TextButtonStyle(b.getStyle()));
        b.getStyle().up = null;
        b.setSize(size);
        b.update(() -> {
            b.setPosition(getWidth() - b.getWidth(), getHeight() - b.getHeight());
            b.toFront();
        });
        addChild(b);
    }

    public void addInGameVisible(){
        visible(() -> Vars.state.isMenu() || Vars.ui.hudfrag.shown);
    }
}
