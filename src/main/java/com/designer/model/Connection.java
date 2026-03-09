package com.designer.model;

import javafx.scene.paint.Color;

public class Connection {

    public enum LineStyle { SOLID, DASHED, ORTHOGONAL, ORTHOGONAL_DASHED }
    public enum EndPointStyle { NONE, ARROW, AGGREGATION, COMPOSITION, CROW_FOOT, HOLLOW_TRIANGLE }

    private FlowNode source;
    private FlowNode target;

    private double srcPctX, srcPctY;
    private double tgtPctX, tgtPctY;

    private boolean selected=false;

    private String nameText="";
    private String srcText="";
    private String tgtText="";

    private double nameOffX = 0;
    private double nameOffY = 0;
    private double orthoOffset = 0;

    private Color lineColor = Color.BLACK;
    private double lineWidth = 2.0;

    public boolean isSelected() { return selected;}
    public void setSelected(boolean selected) { this.selected = selected; }

    private LineStyle lineStyle;
    private EndPointStyle srcEndpointStyle;
    private EndPointStyle tgtEndpointStyle;

    public Connection(FlowNode source, FlowNode target, double srcPctX, double srcPctY, double tgtPctX, double tgtPctY) {
        this.source = source;
        this.target = target;
        this.srcPctX = srcPctX;
        this.srcPctY = srcPctY;
        this.tgtPctX = tgtPctX;
        this.tgtPctY = tgtPctY;

        this.lineStyle=LineStyle.SOLID;
        this.tgtEndpointStyle=EndPointStyle.ARROW;
        this.srcEndpointStyle=EndPointStyle.NONE;
    }

    public FlowNode getSource() { return source; }
    public FlowNode getTarget() { return target; }
    public double getSrcPctX() { return srcPctX; }
    public double getSrcPctY() { return srcPctY; }
    public double getTgtPctX() { return tgtPctX; }
    public double getTgtPctY() { return tgtPctY; }

    public LineStyle getLineStyle() { return lineStyle; }
    public void setLineStyle(LineStyle lineStyle) {this.lineStyle=lineStyle; }
    public EndPointStyle getSrcEndpointStyle() { return srcEndpointStyle; }
    public void setTgtEndpointStyle(EndPointStyle style) {this.tgtEndpointStyle=style;}
    public EndPointStyle getTgtEndpointStyle() { return tgtEndpointStyle;}
    public void setSrcEndpointStyle(EndPointStyle style) { this.srcEndpointStyle=style; }

    public String getName() { return nameText; }
    public void setName(String name) { this.nameText=name; }
    public String getSrcText() { return srcText; }
    public void setSrcText(String srcText) { this.srcText=srcText; }
    public String getTgtText() { return tgtText; }
    public void setTgtText(String tgtText) { this.tgtText=tgtText; }

    public double getNameOffX() { return nameOffX; }
    public void setNameOffX(double nameOffX) { this.nameOffX=nameOffX; }
    public double getNameOffY() { return nameOffY; }
    public void setNameOffY(double nameOffY) { this.nameOffY=nameOffY; }

    public Color getLineColor() { return lineColor; }
    public void setLineColor(Color lineColor) { this.lineColor = lineColor; }
    public double getLineWidth() { return lineWidth; }
    public void setLineWidth(double lineWidth) { this.lineWidth = lineWidth; }
    public double getOrthoOffset() { return orthoOffset; }
    public void setOrthoOffset(double orthoOffset) { this.orthoOffset = orthoOffset; }

    public void setSrcPctX(double srcPctX) { this.srcPctX=srcPctX; }
    public void setSrcPctY(double srcPctY) { this.srcPctY=srcPctY; }
    public void setTgtPctX(double tgtPctX) { this.tgtPctX=tgtPctX; }
    public void setTgtPctY(double tgtPctY) { this.tgtPctY=tgtPctY; }

}