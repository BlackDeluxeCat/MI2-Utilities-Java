package mi2u.ui;

import arc.Core;
import arc.math.Interp;
import arc.scene.Element;
import arc.scene.actions.Actions;
import arc.scene.style.Drawable;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.Align;
import arc.util.Nullable;
import mi2u.MI2UTmp;
import mi2u.MI2UVars;

public class PopupTable extends Table{
    public boolean shown = false;

    public void popup(int align){
        if(shown) return;
        Core.scene.add(this);
        shown = true;
        setTransform(true);
        clearActions();
        float duration = 0.15f;
        if(Align.isTop(align)){
            actions(Actions.scaleTo(1f,0f), Actions.translateBy(0f, getPrefHeight()));
            actions(Actions.parallel(Actions.scaleTo(1f, 1f, duration, Interp.fade), Actions.translateBy(0f , -getPrefHeight(), duration, Interp.fade)));
        }else if(Align.isBottom(align)){
            actions(Actions.scaleTo(1f,0f));
            actions(Actions.parallel(Actions.scaleTo(1f, 1f, duration, Interp.fade)));
        }else if(Align.isLeft(align)){
            actions(Actions.scaleTo(0f,1f));
            actions(Actions.parallel(Actions.scaleTo(1f, 1f, duration, Interp.fade)));
        }else if(Align.isRight(align)){
            actions(Actions.scaleTo(0f,1f), Actions.translateBy(getPrefWidth(), 0f));
            actions(Actions.parallel(Actions.scaleTo(1f, 1f, duration, Interp.fade), Actions.translateBy(-getPrefWidth() , 0f, duration, Interp.fade)));
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
        keepInStage();
        invalidateHierarchy();
        pack();
    }

    public void hideWithoutFocusOn(@Nullable Element...elements){
        boolean hasMouse = false;
        if(elements != null){
            for(Element e : elements){
                if(e.hasMouse()){
                    hasMouse = true;
                    break;
                }
            }
        }
        if(!hasMouse) hide();
    }

    public void addCloseButton(){
        TextButton b = new TextButton("X", MI2UVars.textb){{clicked(() -> hide());}};
        b.setSize(36f);
        b.update(() -> {
            b.setPosition(getPrefWidth() - b.getWidth(), getPrefHeight() - b.getHeight());
            b.toFront();
        });
        addChild(b);
    }
}
