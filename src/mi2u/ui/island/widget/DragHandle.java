package mi2u.ui.island.widget;

import arc.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import mi2u.ui.capability.*;
import mi2u.ui.island.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static mi2u.MI2UVars.buttonSize;

public class DragHandle implements WidgetContent{
    public Label handle;
    public boolean dragging = false;
    final Vec2 vTouchDown = new Vec2();
    final Vec2 vTouchDragging = new Vec2();
    public DragCapabilityEvent dragCapEvent = new DragCapabilityEvent();

    public DragHandle(){
        handle = new Label("" + Iconc.move);
        handle.addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                handle.localToStageCoordinates(vTouchDown.set(x, y));
                handle.setColor(Pal.accent);
                Core.graphics.cursor(Graphics.Cursor.SystemCursor.hand);
                dragCapEvent.setOrigin(vTouchDown.x, vTouchDown.y);
                dragging = true;
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                super.touchDragged(event, x, y, pointer);
                handle.localToStageCoordinates(vTouchDragging.set(x, y));
                dragCapEvent.setDelta(vTouchDragging.x - vTouchDown.x,  vTouchDragging.y - vTouchDown.y);
                handle.fire(dragCapEvent);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                super.touchUp(event, x, y, pointer, button);
                handle.setColor(Color.white);
                Core.graphics.restoreCursor();
                dragging = false;
                event.reset();
            }
        });
    }

    @Override
    public void rebuild(Island island) {
        island.add(handle).minSize(buttonSize);
    }
}
