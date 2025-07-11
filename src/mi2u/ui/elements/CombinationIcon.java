package mi2u.ui.elements;

import arc.func.*;
import arc.scene.ui.layout.*;

public class CombinationIcon extends Stack{
    public CombinationIcon(Cons<Table> center){
        Table table = new Table(t -> t.table(center));
        table.setFillParent(true);
        add(table);
    }

    public CombinationIcon topLeft(Cons<Table> cons){
        Table table = new Table(t -> {
            t.table(cons);
            t.add();
            t.row();
            t.add();
            t.add().grow();
        });
        table.setFillParent(true);
        add(table);
        return this;
    }

    public CombinationIcon topRight(Cons<Table> cons){
        Table table = new Table(t -> {
            t.add();
            t.table(cons);
            t.row();
            t.add().grow();
        });
        table.setFillParent(true);
        add(table);
        return this;
    }

    public CombinationIcon bottomLeft(Cons<Table> cons){
        Table table = new Table(t -> {
            t.add();
            t.add().grow();
            t.row();
            t.table(cons);
        });
        table.setFillParent(true);
        add(table);
        return this;
    }

    public CombinationIcon bottomRight(Cons<Table> cons){
        Table table = new Table(t -> {
            t.add().grow();
            t.add();
            t.row();
            t.add();
            t.table(cons);
        });
        table.setFillParent(true);
        add(table);
        return this;
    }
}
