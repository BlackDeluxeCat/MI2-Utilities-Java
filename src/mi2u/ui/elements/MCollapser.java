package mi2u.ui.elements;

import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;

public class MCollapser extends WidgetGroup{
    Table table;
    @Nullable
    Boolp collapsedFunc;
    private final CollapseAction collapseAction = new CollapseAction();
    boolean collapsed, autoAnimate;
    boolean horizontal, vertical = true;
    boolean actionRunning;
    float currentHeight, currentWidth;
    float seconds = 0.4f;

    public MCollapser(Cons<Table> cons, boolean collapsed){
        this(new Table(), collapsed);
        cons.get(table);
    }

    public MCollapser(Table table, boolean collapsed){
        this.table = table;
        this.collapsed = collapsed;
        setTransform(true);

        updateTouchable();
        addChild(table);
    }

    public MCollapser setDuration(float seconds){
        this.seconds = seconds;
        return this;
    }

    public MCollapser setCollapsed(Boolp collapsed){
        this.collapsedFunc = collapsed;
        return this;
    }

    public MCollapser setCollapsed(boolean autoAnimate, Boolp collapsed){
        this.collapsedFunc = collapsed;
        this.autoAnimate = autoAnimate;
        return this;
    }

    public MCollapser setDirection(boolean horizontal, boolean vertical){
        this.horizontal = horizontal;
        this.vertical = vertical;
        return this;
    }

    public MCollapser setInterpolation(Interp x, Interp y){
        collapseAction.interpolationHorizontal = x;
        collapseAction.interpolationVertical = y;
        return this;
    }

    public MCollapser setInterpolation(Interp interp){
        setInterpolation(interp, interp);
        return this;
    }

    public void toggle(){
        setCollapsed(!isCollapsed());
    }

    public void toggle(boolean animated){
        setCollapsed(!isCollapsed(), animated);
    }

    public void setCollapsed(boolean collapse, boolean withAnimation){
        this.collapsed = collapse;
        updateTouchable();

        if(table == null) return;

        actionRunning = true;

        if(withAnimation){
            addAction(collapseAction);
        }else{
            currentHeight = vertical && collapse ? 0f : table.getPrefHeight();
            currentWidth = horizontal && collapse ? 0f : table.getPrefWidth();
            collapsed = collapse;

            actionRunning = false;
            invalidateHierarchy();
        }
    }

    public void setCollapsed(boolean collapse){
        setCollapsed(collapse, true);
    }

    public boolean isCollapsed(){
        return collapsed;
    }

    private void updateTouchable(){
        Touchable touchable1 = collapsed ? Touchable.disabled : Touchable.enabled;
        this.touchable = touchable1;
    }

    @Override
    public void draw(){
        if(currentHeight > 1 && currentWidth > 1){
            Draw.flush();
            if(clipBegin(x, y, horizontal ? currentWidth : getWidth(), vertical ? currentHeight : getHeight())){
                super.draw();
                Draw.flush();
                clipEnd();
            }
        }
    }

    @Override
    public void act(float delta){
        super.act(delta);

        if(collapsedFunc != null){
            boolean col = collapsedFunc.get();
            if(col != collapsed){
                setCollapsed(col, autoAnimate);
            }
        }
    }

    @Override
    public void layout(){
        if(table == null) return;

        table.setBounds(0, 0, getWidth(), getHeight());

        if(!actionRunning){
            currentHeight = vertical && collapsed ? 0f : table.getPrefHeight();
            currentWidth = horizontal && collapsed ? 0f : table.getPrefWidth();
        }
    }

    @Override
    public float getPrefHeight(){
        if(table == null) return 0;

        if(!actionRunning){
            if(collapsed && vertical)
                return 0;
            else
                return table.getPrefHeight();
        }

        return currentHeight;
    }

    @Override
    public float getPrefWidth(){
        if(table == null) return 0;

        if(!actionRunning){
            if(collapsed && horizontal)
                return 0;
            else
                return table.getPrefWidth();
        }

        return currentWidth;
    }

    public void setTable(Table table){
        this.table = table;
        clearChildren();
        addChild(table);
    }

    @Override
    public float getMinWidth(){
        return 0;
    }

    @Override
    public float getMinHeight(){
        return 0;
    }

    @Override
    protected void childrenChanged(){
        super.childrenChanged();
        if(getChildren().size > 1) throw new ArcRuntimeException("Only one actor can be added to CollapsibleWidget");
    }

    private class CollapseAction extends Action{
        Interp interpolationHorizontal = Interp.exp5, interpolationVertical = Interp.exp5;
        float progress = 0f;
        CollapseAction(){}

        @Override
        public boolean act(float delta){
            if(collapsed){
                progress -= delta / seconds;
                if(progress <= 0f){
                    progress = 0f;
                    actionRunning = false;
                }
            }else{
                progress += delta / seconds;
                if(progress >= 1f){
                    progress = 1f;
                    actionRunning = false;
                }
            }

            if(vertical){
                currentHeight = interpolationVertical.apply(progress) * table.getPrefHeight();
            }

            if(horizontal){
                currentWidth = interpolationHorizontal.apply(progress) * table.getPrefWidth();
            }

            invalidateHierarchy();
            return !actionRunning;
        }
    }
}