package mi2u.ui;

import arc.*;
import arc.math.geom.Vec2;
import arc.scene.*;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mi2u.MI2UTmp;
import mindustry.game.EventType.Trigger;
import mindustry.input.Binding;
import mindustry.ui.Styles;

import static mi2u.MI2UVars.*;
import static mindustry.Vars.*;
/** A temp custom mindow that catch any ui element into its container 
 * for fun
*/
public class CustomContainerMindow extends Mindow2{
    private Seq<ElementItem> items = new Seq<ElementItem>();
    private boolean ticking = false;
    private Table plate = new Table(Styles.black3);
    @Nullable private Element tgt;

    public CustomContainerMindow() {
        super("@container.MI2U", "@customContainer.help");
        Events.run(Trigger.update, () -> {
            if(!ticking && tgt == null){
                plate.remove();
                return;
            }
            Element e = null;
            if(ticking){
                e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
            }
            if(e == null && tgt != null){
                e = tgt;
            }
            if(e == null) return;
            Core.scene.root.addChild(plate);
            Vec2 v = e.localToStageCoordinates(MI2UTmp.v2.set(0, 0));
            plate.touchable = Touchable.disabled;
            plate.setBounds(v.x, v.y, e.getWidth(), e.getHeight());
            plate.setZIndex(10005);
            if(e != null && Core.input.keyDown(Binding.select) && !e.isDescendantOf(this)){
                tgt = e;
                ticking = false;
            }
        });
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();
        cont.table(tt -> {
            tt.add("CAUTION").get().setColor(1, 0, 0, 1);
            tt.button("#Hit", textbtoggle, () -> {
                ticking = !ticking;
                if(!ticking) tgt = null;
            }).width(50).name("SelectButton").update(b -> b.setChecked(ticking));
            tt.button("->Parent", textb, () -> {
                if(tgt == null) return;
                if(tgt.hasParent()) tgt = tgt.parent;
            }).width(50);
            tt.button("+Capture", textb, () -> {
                if(tgt != null && !tgt.isDescendantOf(this)){
                    if(items.contains(i -> i.item == tgt)) return;
                    items.add(new ElementItem(tgt));
                    tgt = null;
                    rebuild();
                }
            }).width(50).disabled(b -> {
                return !(tgt != null && !tgt.isDescendantOf(this));
            });
        });

        cont.row();

        if(items == null) return;
        items.each(item -> {
            cont.button("x", textb, () -> {
                item.originParent.addChild(item.item);
                items.remove(i -> i.item == item.item);
                rebuild();
            }).width(titleButtonSize);
            cont.row();
            cont.add(item.item);
            cont.row();
        });
    }

    public void getElement(float x, float y){

    }
    
    public class ElementItem{
        private Element item;
        @Nullable private Group originParent;
        public ElementItem(Element item){
            this.item = item;
            originParent = this.item.parent;
        }
    }
}
