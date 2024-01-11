package mi2u.ui;

import arc.*;
import arc.func.Boolf;
import arc.func.Cons;
import arc.graphics.Color;
import arc.input.*;
import arc.math.Interp;
import arc.math.Mathf;
import arc.scene.*;
import arc.scene.actions.Actions;
import arc.scene.actions.TemporalAction;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import arc.struct.*;
import arc.struct.Queue;
import arc.util.*;
import mi2u.*;
import mi2u.game.*;
import mi2u.io.*;
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
    public LogicDialog targetLogicDialog;
    LExecutor exec = null, lastexec = null;
    Seq<String> vars = new Seq<>();

    public Table varsBaseTable;
    public Table varsTable;
    public String split = "";
    public int depth = 6;
    PopupTable autoFillVarTable;

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
        setTargetDialog(ui.logic);

        autoFillVarTable = new PopupTable(){
            TextField field;
            String last = "";
            Table buttons;
            ScrollPane pane;
            MI2Utils.IntervalMillis timer = new MI2Utils.IntervalMillis(3);
            int tabIndex = 0;
            boolean hasKey = false, hasMouse = false;
            {
                Core.scene.addListener(new InputListener(){
                    @Override
                    public boolean keyDown(InputEvent event, KeyCode keycode){
                        if(field != null && keycode == KeyCode.tab && MI2USettings.getBool(mindowName + ".autocomplete", true)){
                            if(hasKey){
                                field.setFocusTraversal(false);
                                Core.scene.setKeyboardFocus(field);
                                field.setCursorPosition(field.getText().length());
                                Time.run(2f, () -> {
                                    if(field == null) return;
                                    field.setFocusTraversal(true);
                                });
                            }else{
                                Core.scene.setKeyboardFocus(autoFillVarTable);
                                tabIndex = 0;
                            }
                        }
                        return super.keyDown(event, keycode);
                    }
                });
                touchable = Touchable.enabled;
                background(Styles.black5);
                visible(() -> {
                    hasKey = hasKeyboard();
                    hasMouse = hasMouse();
                    if(timer.get(300)){
                        if(!hasKey){
                            if(targetLogicDialog != null && Core.scene.getKeyboardFocus() instanceof TextField f && targetLogicDialog.isAscendantOf(f)){
                                field = f;
                            }else{
                                field = null;
                            }
                        }
                    }

                    if(field != null){
                        var v = field.localToStageCoordinates(MI2UTmp.v1.setZero());
                        setPosition(v.x, v.y, Align.bottomRight);
                        keepInScreen();
                        setZIndex(1000);
                        if(!last.equals(field.getText())){
                            last = field.getText();
                            build();
                        }
                    }
                    return field != null;
                });

                addListener(new InputListener(){
                    @Override
                    public boolean keyDown(InputEvent event, KeyCode keycode){
                        timer.get(1, 0);
                        if(buttons.getChildren().size > 0){
                            if(keycode == KeyCode.up) tabIndex = Mathf.mod(--tabIndex, buttons.getChildren().size);
                            if(keycode == KeyCode.down) tabIndex = Mathf.mod(++tabIndex, buttons.getChildren().size);
                            if(field != null && keycode == KeyCode.enter){
                                tabIndex = Mathf.mod(tabIndex, buttons.getChildren().size);
                                var button = buttons.getCells().get(tabIndex).get();
                                button.fireClick();
                                Core.scene.setKeyboardFocus(field);
                                field.setCursorPosition(field.getText().length());
                            }
                            //scroll
                            if(keycode == KeyCode.up || keycode == KeyCode.down) scrollToTabIndex();
                        }
                        return super.keyDown(event, keycode);
                    }
                });
            }

            @Override
            public void act(float delta){
                super.act(delta);
                if(timer.check(1, 700) && Core.input.keyDown(KeyCode.up) && timer.get(2, 50)){
                    tabIndex = Mathf.mod(--tabIndex, buttons.getChildren().size);
                    scrollToTabIndex();
                }
                if(timer.check(1, 700) && Core.input.keyDown(KeyCode.down) && timer.get(2, 50)){
                    tabIndex = Mathf.mod(++tabIndex, buttons.getChildren().size);
                    scrollToTabIndex();
                }
            }

            void scrollToTabIndex(){
                var button = buttons.getCells().get(tabIndex).get();
                pane.scrollTo(button.x, button.y, button.getWidth(), button.getHeight());
            }

            void build(){
                clearChildren();
                pane(t -> {
                    buttons = t;
                    String input = field.getText();
                    t.defaults().left().growX().minWidth(100f);
                    int mimi = 0;
                    try{
                        if(!split.equals("")){
                            int blockIndex = Strings.count(input, split);
                            var inputBlocks = input.split(cookSplit(split), depth);
                            ObjectSet<String> used = new ObjectSet<>();
                            varsfor : for(var v : vars){
                                if(Strings.count(v, split) < blockIndex) continue;
                                var varBlocks = v.split(cookSplit(split), depth);
                                if(blockIndex > 0){
                                    for(int i = 0; i < blockIndex; i++){
                                        if(!inputBlocks[i].equals(varBlocks[i])) continue varsfor;
                                    }
                                }
                                if(varBlocks[blockIndex].equals(inputBlocks[blockIndex]) || !varBlocks[blockIndex].startsWith(inputBlocks[blockIndex])) continue;
                                if(used.add(varBlocks[blockIndex])){
                                    int finalMimi = mimi++;
                                    t.add(varBlocks[blockIndex]).with(l -> {
                                        l.clicked(() -> {
                                            var str = Strings.join(split, new Seq<>(false, varBlocks, 0, blockIndex + 1));
                                            field.setText(str);
                                            field.change();
                                        });
                                        l.hovered(() -> tabIndex = finalMimi);
                                        l.update(() -> l.setColor(tabIndex == finalMimi && (hasKey || hasMouse) ? Color.acid : varBlocks.length >= blockIndex + 1 ? Color.royal : Color.white));
                                    });
                                    t.row();
                                }
                            }
                        }else{
                            for(var v : vars){
                                if(!v.startsWith(input)) continue;
                                int finalMimi1 = mimi++;
                                t.add(v).with(l -> {
                                    l.clicked(() -> {
                                        field.setText(v);
                                        field.change();
                                    });
                                    l.hovered(() -> tabIndex = finalMimi1);
                                    l.update(() -> l.setColor(tabIndex == finalMimi1 && (hasKey || hasMouse) ? Color.acid : Color.white));
                                });
                                t.row();
                            }
                        }
                    }catch(Exception ignored){}
                }).minWidth(100f).maxWidth(200f).maxHeight(300f).with(sp -> {
                    pane = sp;
                    sp.setScrollingDisabledX(true);
                    sp.setFadeScrollBars(true);
                    sp.setupFadeScrollBars(0.5f, 0.5f);
                });
            }
        };
        if(MI2USettings.getBool(mindowName + ".autocomplete", true)) autoFillVarTable.popup(Align.topLeft);

        varsBaseTable = new Table();
        varsTable = new Table();
        searchBaseTable = new Table();
        cutPasteBaseTable = new Table();
        backupTable = new Table();
        Events.on(MI2UEvents.FinishSettingInitEvent.class, e -> {
            setupVarsMode(varsBaseTable);
            setupSearchMode(searchBaseTable);
            setupCutPasteMode(cutPasteBaseTable);
            setupBackupMode(backupTable);
        });

        titlePane.table(tt -> {
            tt.defaults().growX().minSize(32f);
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
        });
    }

    @Override
    public void act(float delta){
        super.act(delta);
        if(targetLogicDialog.canvas != null && backupTimer.get(0, 60000)){
            backup(targetLogicDialog.canvas.save());
        }
    }

    @Override
    public void initSettings(){
        super.initSettings();
        settings.add(new MI2USettings.CheckEntry(mindowName + ".autocomplete", "@settings.logicHelper.autocomplete", true, b -> {
            if(b) autoFillVarTable.popup();
            else autoFillVarTable.hide();
        }));
    }

    public void setTargetDialog(LogicDialog ld){
        targetLogicDialog = ld;
    }

    @Override
    public void setupCont(Table cont) {
        cont.clear();
        cont.image().color(Color.pink).growX().height(2f);
        cont.row();
        switch(mode){
            case vars -> cont.add(varsBaseTable).growX();
            case search -> cont.add(searchBaseTable).growX();
            case cutPaste -> cont.add(cutPasteBaseTable).growX();
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
                if(targetLogicDialog.canvas == null) return;
                backup(targetLogicDialog.canvas.save());
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
                        if(targetLogicDialog.canvas == null) return;
                        //backup(canvas.save());
                        targetLogicDialog.canvas.load(str);
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

            t.button("||| " + Iconc.play + " |||", textb, this::doCutPaste).with(funcSetTextb).height(36f).disabled(tb -> !(targetLogicDialog != null && cutStart < targetLogicDialog.canvas.statements.getChildren().size && cutEnd < targetLogicDialog.canvas.statements.getChildren().size && pasteStart <= targetLogicDialog.canvas.statements.getChildren().size && cutEnd >= cutStart)).colspan(2);
        });


    }

    public void doCutPaste(){
        var stats = targetLogicDialog != null ? targetLogicDialog.canvas.statements : null;
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
        if(!keyWord.equals("") && targetLogicDialog != null){
            if(targetLogicDialog.canvas.pane.getWidget() instanceof Table ldt){
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
        if(results.any() && targetLogicDialog != null){
            if(results.remove(rem -> !rem.isDescendantOf(targetLogicDialog))) doSearch();  //if remove works, probably lstatement is changed || lcanvas is rebuilt, so previous TextFields are invalid anymore.
            if(index >= results.size) index = 0;
            if(index < 0) index = results.size - 1;
            Element e = results.get(index);
            e.localToAscendantCoordinates(targetLogicDialog.canvas.pane.getWidget(), MI2UTmp.v2.setZero());
            targetLogicDialog.canvas.pane.scrollTo(MI2UTmp.v2.x, MI2UTmp.v2.y, e.getWidth(), e.getHeight());
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
                tt.field(split, Styles.nodeField, s -> {
                    split = s;
                    rebuildVars(varsTable);
                }).growX().with(f -> {
                    f.setMessageText("@logicHelper.splitField.msg");
                }).minWidth(48f);

                tt.add(((MI2USettings.CheckEntry)MI2USettings.getEntry(mindowName + ".autocomplete")).newTextButton("@settings.logicHelper.autocomplete")).minSize(32f);
            });

            t.row();

            t.pane(varsTable).growX().self(c -> {
                c.maxHeight(Core.graphics.getHeight() / 3f);
            });
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
            vars.clear();
            new Seq<>(exec.vars).each(v -> {if(!v.constant && !v.name.startsWith("___")) this.vars.add(v.name);});
            Sort.instance().sort(vars);

            if(!split.equals("")){
                vars.each(s -> {
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
                vars.each(s -> {
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
