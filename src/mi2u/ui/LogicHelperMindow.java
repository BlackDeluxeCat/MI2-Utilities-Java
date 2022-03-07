package mi2u.ui;

import java.lang.reflect.*;

import arc.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.Seq;
import arc.struct.Sort;
import arc.util.*;
import mindustry.gen.Iconc;
import mindustry.logic.*;
import mindustry.logic.LExecutor.*;
import mindustry.ui.*;

import static mindustry.Vars.*;
import static mi2u.MI2UVars.*;

public class LogicHelperMindow extends Mindow2{
    Table varsTable; 
    TextField f;
    LExecutor exec = null, lastexec = null;
    String split = "";
    int depth = 6;

    public LogicHelperMindow(){
        super("@logicHelper.MI2U", "@logicHelper.help");
        closable = false;
        mindowName = "LogicHelper";
    }

    @Override
    public void setupCont(Table cont){
        cont.clear();
        varsTable = new Table();
        cont.table(t -> {
            t.clear();
            t.table(tt -> {
                f = tt.field(split, Styles.nodeField, s -> {
                    split = s;
                    rebuildVars(varsTable);
                }).fillX().get();
                f.setMessageText("@logicHelper.splitField.msg");

            });

            t.row();

            t.pane(varsTable).growX().maxSize(Core.graphics.getWidth() / 4, Core.graphics.getHeight() / 3);
        });

        cont.update(() -> {
            Field field;
            try {
                field = ui.logic.getClass().getDeclaredField("executor");
                field.setAccessible(true);
                exec = (LExecutor)field.get(ui.logic);
            } catch (Exception e) {
                Log.err(e);
            }
            if(exec != lastexec){
                rebuildVars(varsTable);
                lastexec = exec;
            }
        });

        ui.logic.shown(() -> rebuildVars(varsTable));
    }

    private void rebuildVars(Table tt){
        if(exec != null){
            tt.clear();
            if(!split.equals("")){
                Seq<String> seq = new Seq<>();
                new Seq<Var>(exec.vars).each(v -> {if(!v.constant && !v.name.startsWith("___")) seq.add(v.name);});
                Sort.instance().sort(seq);
            
                seq.each(s -> {
                    tt.button("" + Iconc.paste, textb, () -> {
                        Core.app.setClipboardText(s);
                    }).size(36,24);
            
                    String[] blocks = s.split(cookSplit(split), depth);
                    for(int bi = 0; bi < Math.min(depth, blocks.length); bi++){
                        //if(blocks[bi] == "") continue;
                        String str = blocks[bi] + (bi == blocks.length - 1 ? "":split);
                        tt.button(str, textb, () -> {
                            Core.app.setClipboardText(str);
                        }).left().with(c -> {
                            c.getLabel().setWrap(false);
                            c.getLabelCell().width(Math.min(c.getLabelCell().prefWidth(), 140));
                            c.getLabel().setWrap(true);
                            c.getLabel().setAlignment(Align.left);
                        });
                    }
                    tt.row();
                });
            }else{
                for(int vi = 0; vi < exec.vars.length; vi++){
                    Var lvar = exec.vars[vi];
                    tt.button(lvar.name, textb, () -> {
                        Core.app.setClipboardText(lvar.name);
                    }).growX().get().getLabel().setAlignment(Align.left);
                    tt.row();
                }
            }
        }
    }

    private String cookSplit(String raw){
        String cooking = "";
        for(int si = 0; si < raw.length(); si++){
            if(".$|()[{^?*+\\".indexOf(raw.charAt(si)) != -1) cooking = cooking + "\\";
            cooking = cooking + raw.charAt(si);
        }
        return "(" + cooking + ")";
    }
}
