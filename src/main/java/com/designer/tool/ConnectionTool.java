package com.designer.tool;

import com.designer.model.DiagramModel;
import com.designer.model.FlowNode;
import com.designer.view.MainEditorWindow;
import javafx.animation.PauseTransition;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.awt.event.PaintEvent;


public class ConnectionTool implements  Tool{

    private DiagramModel model;
    private MainEditorWindow view;

    private FlowNode hoveredNode = null;
    private PauseTransition timer;

     public ConnectionTool(DiagramModel model, MainEditorWindow view)
     {
         this.model=model;
         this.view=view;

         timer=new PauseTransition(Duration.millis(500));

         timer.setOnFinished( e-> {
             if(hoveredNode!=null)
             {
                 System.out.println("Au trecut 500ms");
             }
         });

     }

     public void onMouseMoved(MouseEvent e)
     {
         FlowNode nodeUnderMouse=model.findNodeAt(e.getX(),e.getY());
         if(nodeUnderMouse!=hoveredNode)
         {
             hoveredNode=nodeUnderMouse;
             timer.stop();
             if(hoveredNode!=null)
             {
                 timer.playFromStart();
             }
             else
             {
                 System.out.println("ascund x-urile");
             }
         }
     }

     @Override
     public void onMouseDown(MouseEvent e)
     {

     }

     @Override
     public void onMouseDragged(MouseEvent e)
     {

     }

     @Override
     public void onMouseReleased(MouseEvent e)
     {

     }

}