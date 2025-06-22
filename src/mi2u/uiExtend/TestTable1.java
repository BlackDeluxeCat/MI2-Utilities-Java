package mi2u.uiExtend;

import arc.scene.ui.layout.Table;
import mindustry.ui.Styles;

public class TestTable1 extends SnapWindow{
    public TestTable1() {
        super("test-1");
    }

    @Override
    public void setupCont(Table cont) {
        cont.clear();

        cont.table(t -> {
            t.pane(Styles.smallPane, table -> {
                table.label(() -> "TEST LABEL 1");
            }).size(200, 150).scrollX(false);
        }).maxHeight(150);
    }

    @Override
    public void initSettings() {
        super.initSettings();
    }
}
