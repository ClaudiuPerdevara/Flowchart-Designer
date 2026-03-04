package com.designer.model;

import javax.sound.sampled.Line;

public class Connection {

    public enum LineStyle { SOLID, DASHED }
    public enum EndPointStyle { NONE, ARROW, AGGREGATION, COMPOSITION, CROW_FOOT }

    private FlowNode source;
    private FlowNode target;

    private double srcPctX, srcPctY;
    private double tgtPctX, tgtPctY;

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


}