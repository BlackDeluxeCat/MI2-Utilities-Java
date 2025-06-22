package mi2u.uiExtend;

import arc.scene.ui.layout.Table;
import mindustry.ui.Styles;

public class TestTable3 extends SnapWindow{
    public TestTable3() {
        super("test-3");
    }

    @Override
    public void setupCont(Table cont) {
        cont.clear();

        cont.table(t -> {
            t.pane(Styles.smallPane, table -> {
                table.label(() -> "TEST LABEL 3");
            }).size(200, 300).scrollX(false);
        }).maxHeight(150);
    }

    @Override
    public void initSettings() {
        super.initSettings();
    }
}

