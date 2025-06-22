package mi2u.uiExtend;

import arc.scene.ui.layout.Table;
import mindustry.ui.Styles;

public class TestTable4 extends SnapWindow{
    public TestTable4() {
        super("test-4");
    }

    @Override
    public void setupCont(Table cont) {
        cont.clear();

        cont.table(t -> {
            t.pane(Styles.smallPane, table -> {
                table.label(() -> "TEST LABEL 4");
            }).size(200, 400).scrollX(false);
        }).maxHeight(150);
    }

    @Override
    public void initSettings() {
        super.initSettings();
    }
}

