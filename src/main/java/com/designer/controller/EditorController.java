package com.designer.controller;

import com.designer.model.DiagramModel;
import com.designer.model.FlowNode;
import com.designer.tool.*;
import com.designer.view.MainEditorWindow;

public class EditorController {
    private DiagramModel model;
    private MainEditorWindow view;
    private Tool currentTool;

    public EditorController(DiagramModel model, MainEditorWindow view)
    {
        this.model = model;
        this.view = view;
        setupEvents();
    }

    private void setupEvents()
    {

        view.getBtnRect().setOnAction(e -> {
            this.setTool(new RectangleTool(model,view));
            System.out.println("Unealta: rectangle");
        });

        view.getBtnSelect().setOnAction(e -> {
            this.setTool(new SelectionTool(model,view));
            System.out.println("Unealta: selection");
        });

        view.getBtnDiam().setOnAction(e -> {
            this.setTool(new DiamondTool(model,view));
            System.out.println("Unealta: diamond");
        });


        view.getCanvasArea().setOnMouseMoved(e -> {
            if(currentTool != null) currentTool.onMouseMoved(e);
        });

        /// semnale mouse

        view.getCanvasArea().setOnMousePressed(e -> {
            view.getCanvasArea().requestFocus();
            if(currentTool != null) currentTool.onMouseDown(e);
        });

        view.getCanvasArea().setOnMouseDragged(e -> {
            if(currentTool != null) currentTool.onMouseDragged(e);
        });

        view.getCanvasArea().setOnMouseReleased(e -> {
            if(currentTool != null) currentTool.onMouseReleased(e);
        });

        /// semnale tastatura

        view.getCanvasArea().setFocusTraversable(true);

        view.getCanvasArea().setOnKeyPressed(e -> {
            if (currentTool != null) {
                currentTool.onKeyPressed(e);
            }
        });

    }

    public void setTool(Tool tool)
    {
        this.currentTool = tool;
    }
}
