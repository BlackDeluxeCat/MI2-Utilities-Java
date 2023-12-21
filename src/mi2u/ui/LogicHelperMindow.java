package mi2u.ui;

import arc.Core;
import arc.func.Boolf;
import arc.func.Cons;
import arc.graphics.Color;
import arc.math.Interp;
import arc.math.Mathf;
import arc.scene.*;
import arc.scene.actions.Actions;
import arc.scene.actions.TemporalAction;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.struct.*;
import arc.util.*;
import mi2u.*;
import mi2u.ui.elements.*;
import mindustry.gen.Iconc;
import mindustry.logic.LCanvas;
import mindustry.logic.LExecutor;
import mindustry.logic.LStatements;
import mindustry.logic.LogicDialog;
import mindustry.ui.Styles;

import static mi2u.MI2UVars.*;
import static mindustry.Vars.*;

public class LogicHelperMindow extends Mindow2{
    public Mode mode;

    public Table varsBaseTable;
    public Table varsTable;
    TextField f;
    LExecutor exec = null, lastexec = null;
    public String split = "";
    public int depth = 6;

    public Table searchBaseTable;
    public String keyWord = "", replace = "";
    boolean wholeWordsMatch = false, caseMatch = false;
    Seq<Element> results = new Seq<>();
    int index = 0;

    public Table cutPasteBaseTable;
    int cutStart = 0, cutEnd = 0, pasteStart = 0;
    boolean copyCode = false, transJump = true;

    public Table backupTable;
    public boolean shouldRebuildBackups = true;
    public MI2Utils.IntervalMillis backupTimer = new MI2Utils.IntervalMillis();
    public Queue<String> backups = new Queue<>(30);

    public LogicHelperMindow(){
        super("LogicHelper", "@logicHelper.MI2U", "@logicHelper.help");
        mode = Mode.vars;
        varsBaseTable = new Table();
        varsTable = new Table();
        searchBaseTable = new Table();
        cutPasteBaseTable = new Table();
        backupTable = new Table();
        setupVarsMode(varsBaseTable);
        setupSearchMode(searchBaseTable);
        setupCutPasteMode(cutPasteBaseTable);
        setupBackupMode(backupTable);

        update(() -> {
            var canvas = parent instanceof LogicDialog ld ? ld.canvas : null;
            if(canvas != null && backupTimer.get(0, 60000)){
                backup(canvas.save());
            }
        });
    }

    @Override
    public void setupCont(Table cont) {
        cont.clear();
        cont.table(tt -> {
            tt.defaults().growX().minSize(48f);
            tt.button("" + Iconc.list, textbtoggle, () -> {
                mode = Mode.vars;
                setupCont(cont);
            }).update(b -> b.setChecked(mode == Mode.vars)).with(funcSetTextb);

            tt.button("" + Iconc.zoom, textbtoggle, () -> {
                mode = Mode.search;
                setupCont(cont);
            }).update(b -> b.setChecked(mode == Mode.search)).with(funcSetTextb);

            tt.button("" + Iconc.copy, textbtoggle, () -> {
                mode = Mode.cutPaste;
                setupCont(cont);
            }).update(b -> b.setChecked(mode == Mode.cutPaste)).with(funcSetTextb);

            tt.button("" + Iconc.save, textbtoggle, () -> {
                mode = Mode.backup;
                setupCont(cont);
            }).update(b -> b.setChecked(mode == Mode.backup)).with(funcSetTextb);
        }).fillX();
        cont.row();
        cont.image().color(Color.pink).growX().height(2f);
        cont.row();
        switch(mode){
            case vars -> cont.add(varsBaseTable);
            case search -> cont.add(searchBaseTable);
            case cutPaste -> cont.add(cutPasteBaseTable);
            case backup -> cont.add(backupTable).growX();
        }
    }

    public void setupBackupMode(Table cont){
        cont.table(t -> {
            t.button("" + Iconc.cancel, textb, () -> {
                backups.clear();
                shouldRebuildBackups = true;
            }).size(32f);
            t.image().color(Color.pink).width(2f).growY();

            t.button(Iconc.save + "Backup Now", textb, () -> {
                var canvas = parent instanceof LogicDialog ld ? ld.canvas : null;
                if(canvas == null) return;
                backup(canvas.save());
            }).update(tb -> {
                tb.getLabel().setWrap(false);
                tb.setText(Iconc.save + Core.bundle.get("logichelper.backup.backupIn") + Strings.fixed(60 - backupTimer.getTime(0) / 1000f, 1));
            }).growX().height(32f);
        }).growX();

        cont.row();

        cont.pane(t -> {
            t.update(() -> {
                if(!shouldRebuildBackups) return;
                shouldRebuildBackups = false;
                t.clearChildren();
                backups.each(str -> {
                    t.labelWrap(str.substring(0, Math.min(str.length(), 30))).fontScale(0.5f).growX();
                    t.button("" + Iconc.redo, textb, () -> {
                        var canvas = parent instanceof LogicDialog ld ? ld.canvas : null;
                        if(canvas == null) return;
                        //backup(canvas.save());
                        canvas.load(str);
                    }).size(32f);
                    t.row();
                });
            });
        }).growX().maxHeight(400f);
    }

    public void backup(String str){
        backups.add(str + "");
        shouldRebuildBackups = true;
    }

    public void setupCutPasteMode(Table cont){
        cont.table(t -> {
            t.add("@logicHelper.cutPaste.start");
            t.field("", Styles.nodeField, s -> cutStart = Mathf.clamp(Strings.parseInt(s), 0, 1000)).fillX().width(100f)
                    .update(tf -> tf.setText(String.valueOf(cutStart))).get().setFilter(TextField.TextFieldFilter.digitsOnly);
            t.add("@logicHelper.cutPaste.end");
            t.field("", Styles.nodeField, s -> cutEnd = Mathf.clamp(Strings.parseInt(s), 0, 1000)).fillX().width(100f)
                    .update(tf -> tf.setText(String.valueOf(cutEnd))).get().setFilter(TextField.TextFieldFilter.digitsOnly);
        });
        cont.row();

        cont.table(t -> {
            t.add("@logicHelper.cutPaste.to");
            t.field("", Styles.nodeField, s -> pasteStart = Mathf.clamp(Strings.parseInt(s),0, 1000)).fillX().width(100f)
                    .update(tf -> tf.setText(String.valueOf(pasteStart))).
                    with(tf -> {
                       tf.setFilter(TextField.TextFieldFilter.digitsOnly);
                    });

        });
        cont.row();

        cont.table(t -> {
            t.defaults().growX();
            t.button("" + Iconc.copy + Core.bundle.get("logicHelper.cutPaste.copyMode"), textbtoggle, () -> {
                copyCode = !copyCode;
            }).update(b -> b.setChecked(copyCode)).with(funcSetTextb).height(36f);

            t.button("" + Iconc.move + Core.bundle.get("logicHelper.cutPaste.transJump"), textbtoggle, () -> {
                transJump = !transJump;
            }).update(b -> b.setChecked(transJump)).with(funcSetTextb).height(36f).disabled(tb -> !copyCode);

            t.row();

            t.button("||| " + Iconc.play + " |||", textb, this::doCutPaste).with(funcSetTextb).height(36f).disabled(tb -> !(parent instanceof LogicDialog ld && cutStart < ld.canvas.statements.getChildren().size && cutEnd < ld.canvas.statements.getChildren().size && pasteStart <= ld.canvas.statements.getChildren().size && cutEnd >= cutStart)).colspan(2);
        });


    }

    public void doCutPaste(){
        var stats = parent instanceof LogicDialog ld ? ld.canvas.statements : null;
        if(stats == null) return;
        int times = cutEnd - cutStart + 1;
        int ind = 0;
        ObjectMap<LCanvas.StatementElem, Integer> toSetup = new ObjectMap<>();
        do{
            LCanvas.StatementElem dragging = null;
            if(cutStart > pasteStart){
                if(copyCode){
                    dragging = (LCanvas.StatementElem)stats.getChildren().get(cutEnd);
                    dragging.copy();
                    dragging = (LCanvas.StatementElem)stats.getChildren().get(cutEnd + 1);
                    if(dragging.st instanceof LStatements.JumpStatement jp){
                        toSetup.put(dragging, jp.destIndex - dragging.index + (jp.destIndex < dragging.index? 1:0));
                    }
                    dragging.remove();
                    stats.addChildAt(pasteStart, dragging);
                }else{
                    dragging = (LCanvas.StatementElem)stats.getChildren().get(cutEnd);
                    dragging.remove();
                    stats.addChildAt(pasteStart, dragging);
                };
            }else if(cutStart < pasteStart){
                if(copyCode){
                    dragging = (LCanvas.StatementElem)stats.getChildren().get(cutStart + ind);
                    dragging.copy();
                    dragging = (LCanvas.StatementElem)stats.getChildren().get(cutStart + ind + 1);
                    if(dragging.st instanceof LStatements.JumpStatement jp){
                        toSetup.put(dragging, jp.destIndex - dragging.index + (jp.destIndex < dragging.index? 1:0));
                    }
                    dragging.remove();
                    stats.addChildAt(pasteStart + ind, dragging);
                }else{
                    dragging = (LCanvas.StatementElem)stats.getChildren().get(cutStart);
                    dragging.remove();
                    stats.addChildAt(pasteStart - 1, dragging);
                };
            }
            stats.layout();
            if(dragging != null) blinkElement(dragging);
        }while(++ind < times);
        if(transJump){
            toSetup.each((se, delta) -> {
                if(se.st instanceof LStatements.JumpStatement jp){
                    jp.destIndex = se.index + delta;
                    jp.setupUI();
                }
            });
        }
    }

    public void setupSearchMode(Table cont){
        cont.clear();
        cont.table(tt -> {
            tt.field("", Styles.nodeField, s -> {
                keyWord = s;
                doSearch();
            }).fillX();

            tt.button("Cc", textbtoggle, () -> {
                caseMatch = !caseMatch;
                doSearch();
                locateElement(null);
            }).update(b -> b.setChecked(caseMatch)).with(funcSetTextb).size(36f);

            tt.button("W", textbtoggle, () -> {
                wholeWordsMatch = !wholeWordsMatch;
                doSearch();
                locateElement(null);
            }).update(b -> b.setChecked(wholeWordsMatch)).with(funcSetTextb).size(36f);
        }).fillX();
        cont.row();

        cont.table(tt -> {
            tt.label(() -> "" + (results.isEmpty()?"NaN/0":(index+1)+"/"+results.size)).growX().get().setColor(Color.gray);
            tt.button("" + Iconc.up + Core.bundle.get("logicHelper.search.prev"), textb, () -> {
                index--;
                locateElement(null);
            }).with(funcSetTextb).height(36f);
            tt.button("" + Iconc.down + Core.bundle.get("logicHelper.search.next"), textb, () -> {
                index++;
                locateElement(null);
            }).with(funcSetTextb).height(36f);
        }).fillX();
        cont.row();

        cont.table(tt -> {
            tt.field("", Styles.nodeField, s -> replace = s).fillX();

            tt.table(ttt -> {
                ttt.defaults().fillX();
                ttt.button("@logicHelper.search.replace", textb, () -> {
                    if(results.isEmpty()) doSearch();
                    locateElement(e -> {
                        if(e instanceof TextField tf){
                            String matched = findMatch(keyWord, tf.getText());
                            if(matched == null) return;
                            tf.setText(Strings.replace(new StringBuilder(tf.getText()), matched, replace).toString());
                            tf.change();
                        }
                    });
                    doSearch();
                    if(index >= results.size) index = 0;
                }).with(funcSetTextb).height(36f);
                ttt.row();

                ttt.button("@logicHelper.search.replaceAll", textb, () -> {
                    if(results.isEmpty()) doSearch();
                    results.each(e -> {
                        if(e instanceof TextField tf){
                            String matched = findMatch(keyWord, tf.getText());
                            if(matched == null) return;
                            tf.setText(Strings.replace(new StringBuilder(tf.getText()), matched, replace).toString());
                            tf.change();
                        }
                    });
                    doSearch();
                }).with(funcSetTextb).height(36f);
            });
        }).fillX();
    }

    private void doSearch(){
        if(!keyWord.equals("") && parent instanceof LogicDialog ld){
            if(ld.canvas.pane.getWidget() instanceof Table ldt){
                results.clear();
                ldt.getChildren().each(e -> deepSelect(e, e2 -> e2 instanceof TextField tf && findMatch(keyWord, tf.getText()) != null));
            }
        }else{
            results.clear();
        }
    }

    private String findMatch(String key, String value){
        if(wholeWordsMatch && value.equals(key)){
            return key;
        }else if(caseMatch && value.contains(key)){
            return key;
        }else{
            String lowerKey = key.toLowerCase();
            String lowerValue = value.toLowerCase();
            int i = lowerValue.indexOf(lowerKey);
            if(i != -1){
                return lowerValue.substring(i, i + lowerKey.length());
            }
        }
        return null;
    }

    private void deepSelect(Element e, Boolf<Element> pred){
        if(e instanceof Group group){
            group.getChildren().each(e2 -> deepSelect(e2, pred));
        }
        if(pred.get(e)) results.add(e);
    }

    private void locateElement(Cons<Element> cons){
        if(results.any() && parent instanceof LogicDialog ld){
            if(results.remove(rem -> !rem.isDescendantOf(ld))) doSearch();  //if remove works, probably lstatement is changed || lcanvas is rebuilt, so previous TextFields are invalid anymore.
            if(index >= results.size) index = 0;
            if(index < 0) index = results.size - 1;
            Element e = results.get(index);
            e.localToAscendantCoordinates(ld.canvas.pane.getWidget(), MI2UTmp.v2.setZero());
            //may not fit UI scaling config
            ld.canvas.pane.setScrollPercentY(1 - (MI2UTmp.v2.y-0.5f*ld.canvas.pane.getScrollHeight())/(ld.canvas.pane.getWidget().getPrefHeight()-ld.canvas.pane.getScrollHeight()));
            blinkElement(e);
            if(e instanceof TextField tf) {
                tf.requestKeyboard();
                tf.selectAll();
            };
            if(cons != null) cons.get(e);
        }
    }

    private void blinkElement(Element e){
        MI2UTmp.c2.set(e.color);
        e.getActions().each(act -> {
            if(act instanceof TemporalAction tm) tm.finish();
        });
        e.actions(Actions.delay(1f), Actions.color(Color.acid, 0.2f), Actions.color(MI2UTmp.c2, 0.5f, Interp.fade), Actions.color(Color.acid, 0.2f), Actions.color(MI2UTmp.c2, 0.5f, Interp.fade));
    }

    public void setupVarsMode(Table cont){
        cont.clear();
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

            t.pane(varsTable).growX().maxHeight(Core.graphics.getHeight() / 3f);
        }).growX();

        cont.update(() -> {
            exec = Reflect.get(ui.logic, "executor");
            if(exec != lastexec){
                rebuildVars(varsTable);
                lastexec = exec;
            }
        });

        rebuildVars(varsTable);
        ui.logic.shown(() -> rebuildVars(varsTable));
    }

    private void rebuildVars(Table tt){
        if(exec != null){
            tt.clear();
            if(!split.equals("")){
                Seq<String> seq = new Seq<>();
                new Seq<>(exec.vars).each(v -> {if(!v.constant && !v.name.startsWith("___")) seq.add(v.name);});
                Sort.instance().sort(seq);
            
                seq.each(s -> {
                    tt.button("" + Iconc.paste, textb, () -> Core.app.setClipboardText(s)).size(36,24);
            
                    String[] blocks = s.split(cookSplit(split), depth);
                    for(int bi = 0; bi < Math.min(depth, blocks.length); bi++){
                        //if(blocks[bi] == "") continue;
                        String str = blocks[bi] + (bi == blocks.length - 1 ? "":split);
                        tt.button(str, textb, () -> Core.app.setClipboardText(str)).left().with(c -> {
                            c.getLabel().setWrap(false);
                            c.getLabelCell().width(Math.min(c.getLabelCell().prefWidth(), 140));
                            c.getLabel().setWrap(true);
                            c.getLabel().setAlignment(Align.left);
                        });
                    }
                    tt.row();
                });
            }else{
                Seq<String> seq = new Seq<>();
                new Seq<>(exec.vars).each(v -> seq.add(v.name));
                Sort.instance().sort(seq);

                seq.each(s -> {
                    tt.button(s, textb, () -> Core.app.setClipboardText(s)).growX().get().getLabel().setAlignment(Align.left);
                    tt.row();
                });
            }
        }
    }

    private String cookSplit(String raw){
        StringBuilder cooking = new StringBuilder();
        for(int si = 0; si < raw.length(); si++){
            if(".$|()[{^?*+\\".indexOf(raw.charAt(si)) != -1) cooking.append("\\");
            cooking.append(raw.charAt(si));
        }
        return "(" + cooking + ")";
    }



    public enum Mode{
        vars, search, cutPaste, backup
    }
}
