package com.designer.model;

public class Connection {
    private FlowNode source;
    private FlowNode target;

    private double srcPctX, srcPctY;
    private double tgtPctX, tgtPctY;

    public Connection(FlowNode source, FlowNode target, double srcPctX, double srcPctY, double tgtPctX, double tgtPctY) {
        this.source = source;
        this.target = target;
        this.srcPctX = srcPctX;
        this.srcPctY = srcPctY;
        this.tgtPctX = tgtPctX;
        this.tgtPctY = tgtPctY;
    }

    public FlowNode getSource() { return source; }
    public FlowNode getTarget() { return target; }
    public double getSrcPctX() { return srcPctX; }
    public double getSrcPctY() { return srcPctY; }
    public double getTgtPctX() { return tgtPctX; }
    public double getTgtPctY() { return tgtPctY; }
}