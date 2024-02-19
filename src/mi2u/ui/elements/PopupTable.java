package mi2u.ui.elements;

import arc.*;
import arc.graphics.*;
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
    public float popupDuration = 0.15f;
    public boolean cancelDrag;

    public void popup(int align){
        if(shown) return;
        Core.scene.add(this);
        shown = true;
        setTransform(true);
        clearActions();
        if(Align.isTop(align)){
            actions(Actions.scaleTo(1f,0f), Actions.translateBy(0f, getPrefHeight()));
            actions(Actions.parallel(Actions.scaleTo(1f, 1f, popupDuration, Interp.fade), Actions.translateBy(0f , -getPrefHeight(), popupDuration, Interp.fade), Actions.run(this::keepInScreen)));
        }else if(Align.isBottom(align)){
            actions(Actions.scaleTo(1f,0f));
            actions(Actions.parallel(Actions.scaleTo(1f, 1f, popupDuration, Interp.fade), Actions.run(this::keepInScreen)));
        }else if(Align.isLeft(align)){
            actions(Actions.scaleTo(0f,1f));
            actions(Actions.parallel(Actions.scaleTo(1f, 1f, popupDuration, Interp.fade), Actions.run(this::keepInScreen)));
        }else if(Align.isRight(align)){
            actions(Actions.scaleTo(0f,1f), Actions.translateBy(getPrefWidth(), 0f));
            actions(Actions.parallel(Actions.scaleTo(1f, 1f, popupDuration, Interp.fade), Actions.translateBy(-getPrefWidth() , 0f, popupDuration, Interp.fade), Actions.run(this::keepInScreen)));
        }
    }

    public void popup(){popup(Align.left);}

    public void hide(){
        if(!shown) return;
        shown = false;
        actions(Actions.scaleTo(1f,0f, popupDuration, Interp.fade), Actions.delay(popupDuration), Actions.remove());
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

    public void addDragBar(){
        addDragBar(20f, Color.acid);
    }

    public void addDragBar(float height, Color color){
        image().growX().height(height).color(color).get().addListener(new InputListener(){
            float fromx, fromy;
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                fromx = x;
                fromy = y;
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                if(cancelDrag) return;
                Vec2 v = localToStageCoordinates(MI2UTmp.v1.set(x, y));
                setPositionInScreen(v.x - fromx, v.y - fromy);
            }
        });
    }

    public void addDragMove(){
        addListener(new InputListener(){
            float fromx, fromy;
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                fromx = x;
                fromy = y;
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                if(cancelDrag) return;
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
