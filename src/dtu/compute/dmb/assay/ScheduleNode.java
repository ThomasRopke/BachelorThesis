package dtu.compute.dmb.assay;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;

public class ScheduleNode implements Comparable<ScheduleNode>, Cloneable {
	
	private int time;
	private AssayNode assayNode;
	private Module module;
	private Point placement;
	
	public ScheduleNode(int time, AssayNode assayNode, Module module, Point placement) {
		this.time = time;
		this.assayNode = assayNode;
		this.module = module;
		this.placement = placement;
	}
	
	@Override
	public int compareTo(ScheduleNode o) {
		return (int) Math.signum(this.time - o.time);
	}
	
	public void setStartTime(int time) {
		this.time = time;
	}
	
	public int getStartTime() {
		return time;
	}
	
	public int getEndTime() {
		return time + module.getTime();
	}
	
	public AssayNode getAssayNode() {
		return assayNode;
	}
	
	public Module getModule() {
		return module;
	}
	
	public void setModule(Module module) {
		this.module = module;
	}
	
	public Color getColor() {
		return getAssayNode().getColor();
	}
	
	public Rectangle getBounds() {
		return new Rectangle(placement.x, placement.y, module.getWidth(), module.getHeight());
	}
	
	public Rectangle getMixingBounds() {
		return new Rectangle(placement.x+1, placement.y+1, module.getWidth()-2, module.getHeight()-2);
	}
	
	public int getMixingArea() {
		Rectangle bounds = getMixingBounds();
		return bounds.width * bounds.height;
	}
	
	public Point getCenter() {
		Rectangle area = getBounds();
		return new Point((int)area.getCenterX(), (int)area.getCenterY());
	}
	
	@Override
	public String toString() {
		return time + " " + module.toString() + " " + assayNode.toString();
	}
	
	@Override
	public ScheduleNode clone() {
		return new ScheduleNode(time, assayNode, module, placement);
	}

}
