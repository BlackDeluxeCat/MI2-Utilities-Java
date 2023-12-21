package mi2u.ui.elements;

import arc.math.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.struct.*;

public class TabsTable extends Table{
    Element expand = null;
    boolean horizontal;

    public TabsTable(boolean horizontal){
        this.horizontal = horizontal;
    }

    public void queue(Table table){
        var coll = new MCollapser(table, true);
        coll.setCollapsed(true, () -> expand != coll);
        coll.setDirection(true, true);
        if(horizontal){
            coll.setInterpolation(Interp.smoother, Interp.exp10Out);
        }else{
            coll.setInterpolation(Interp.exp10Out, Interp.smoother);
        }
        coll.setDuration(0.2f);
        add(coll);
        if(!horizontal) row();
    }

    public void queue(Table... tables){
        for(var t : tables) queue(t);
    }

    public void toggle(int index){
        var cell = getCells().get(index);
        cell.grow();
        expand = cell.get();
        getCells().each(c -> c != cell, c -> c.expand(false, false));
    }
}
