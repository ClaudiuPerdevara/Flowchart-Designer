package com.designer.controller;

import com.designer.model.DiagramModel;
import com.designer.model.FlowNode;
import com.designer.model.Connection;
import com.designer.tool.*;
import com.designer.view.MainEditorWindow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.TextInputControl;

import java.util.ArrayList;
import java.util.List;

public class EditorController {
    private DiagramModel model;
    private MainEditorWindow view;
    private Tool currentTool;

    private List<FlowNode> clipboardNodes = new ArrayList<>();

    public EditorController(DiagramModel model, MainEditorWindow view) {
        this.model = model;
        this.view = view;
        setupEvents();

        this.setTool(new SelectionTool(model, view));
    }

    private void setupEvents() {
        view.getBtnRect().setOnAction(e -> this.setTool(new RectangleTool(model, view)));
        view.getBtnSelect().setOnAction(e -> this.setTool(new SelectionTool(model, view)));
        view.getBtnDiam().setOnAction(e -> this.setTool(new DiamondTool(model, view)));
        view.getBtnClass().setOnAction(e -> this.setTool(new ClassTool(view, model)));
        view.getBtnActor().setOnAction(e -> this.setTool(new ActorTool(view, model)));

        view.getCanvasArea().setOnMouseMoved(e -> {
            if (currentTool != null) currentTool.onMouseMoved(e);
        });

        view.getCanvasArea().setOnMousePressed(e -> {
            view.getCanvasArea().requestFocus();
            if (currentTool != null) {
                currentTool.onMouseDown(e);
                // NOU: Am adăugat ClassTool și ActorTool aici
                if (currentTool instanceof RectangleTool || currentTool instanceof DiamondTool ||
                        currentTool instanceof ClassTool || currentTool instanceof ActorTool) {
                    setTool(new SelectionTool(model, view));
                }
            }
        });

        view.getCanvasArea().setOnMouseDragged(e -> {
            if (currentTool != null) currentTool.onMouseDragged(e);
        });
        view.getCanvasArea().setOnMouseReleased(e -> {
            if (currentTool != null) currentTool.onMouseReleased(e);
        });

        // SEMNALE TASTATURĂ GLOBALE (DELETE, COPY, PASTE)
        view.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            // Nu ștergem/copiem forme dacă utilizatorul doar scrie într-o căsuță text din dreapta
            if (e.getTarget() instanceof TextInputControl) return;

            // --- COPY (Ctrl + C) ---
            if (e.isControlDown() && e.getCode() == KeyCode.C) {
                clipboardNodes.clear();
                for (FlowNode n : model.getNodes()) {
                    if (n.isSelected()) clipboardNodes.add(n);
                }
                e.consume();
            }
            // --- PASTE (Ctrl + V) ---
            else if (e.isControlDown() && e.getCode() == KeyCode.V) {
                if (clipboardNodes.isEmpty()) return;

                // Deselectăm tot pentru a selecta doar copiile noi
                for (FlowNode n : model.getNodes()) n.setSelected(false);
                for (Connection c : model.getConnections()) c.setSelected(false);

                List<FlowNode> newNodes = new ArrayList<>();
                for (FlowNode n : clipboardNodes) {

                    FlowNode copy;

                    if (n instanceof com.designer.model.RectangleNode) {
                        copy = new com.designer.model.RectangleNode(n.getX() + 30, n.getY() + 30, n.getWidth(), n.getHeight(), n.getText());
                    } else if (n instanceof com.designer.model.ClassNode) {
                        copy = new com.designer.model.ClassNode(n.getX() + 30, n.getY() + 30, n.getWidth(), n.getHeight(), n.getText());
                    } else if (n instanceof com.designer.model.ActorNode) {
                        copy = new com.designer.model.ActorNode(n.getX() + 30, n.getY() + 30, n.getWidth(), n.getHeight(), n.getText());
                    } else {
                        copy = new com.designer.model.DiamondNode(n.getX() + 30, n.getY() + 30, n.getWidth(), n.getHeight(), n.getText());
                    }

                    copy.setRotation(n.getRotation());
                    copy.setFillColor(n.getFillColor());
                    copy.setStrokeColor(n.getStrokeColor());
                    copy.setStrokeWidth(n.getStrokeWidth());
                    copy.setFontSize(n.getFontSize());
                    copy.setBold(n.isBold());
                    copy.setItalic(n.isItalic());
                    copy.setTextColor(n.getTextColor());

                    copy.setSelected(true);
                    model.addNode(copy);
                    newNodes.add(copy);
                }

                // Noul clipboard devin copiile (pentru a putea da paste de mai multe ori consecutiv)
                clipboardNodes = newNodes;

                if (!newNodes.isEmpty()) view.showNodeProperties(newNodes.get(newNodes.size() - 1));
                view.drawDiagram();
                e.consume();
            }
            // --- DELETE ---
            else if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                boolean needsRedraw = false;

                List<FlowNode> nodesToRemove = new ArrayList<>();
                for (FlowNode n : model.getNodes()) if (n.isSelected()) nodesToRemove.add(n);
                for (FlowNode n : nodesToRemove) {
                    model.removeNode(n);
                    needsRedraw = true;
                }

                List<Connection> connsToRemove = new ArrayList<>();
                for (Connection c : model.getConnections()) if (c.isSelected()) connsToRemove.add(c);
                for (Connection c : connsToRemove) {
                    model.removeConnection(c);
                    needsRedraw = true;
                }

                if (needsRedraw) {
                    view.showDefaultProperties();
                    view.drawDiagram();
                }
                e.consume();
            }
        });
    }

    public void setTool(Tool tool) {
        this.currentTool = tool;
    }
}