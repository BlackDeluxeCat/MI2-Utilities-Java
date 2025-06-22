package mi2u.uiExtend;

import arc.scene.ui.layout.Table;
import mindustry.ui.Styles;

public class TestTable2 extends SnapWindow{
    public TestTable2() {
        super("test-2");
    }

    @Override
    public void setupCont(Table cont) {
        cont.clear();

        cont.table(t -> {
            t.pane(Styles.smallPane, table -> {
                table.label(() -> "TEST LABEL 2");
            }).size(200, 200).scrollX(false);
        }).maxHeight(150);
    }

    @Override
    public void initSettings() {
        super.initSettings();
    }
}

