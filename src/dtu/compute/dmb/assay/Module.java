package dtu.compute.dmb.assay;

import java.awt.Dimension;

public class Module implements Cloneable {
	private Dimension size;
	private int time;
	
	public Module(Dimension size, int time) {
		this.size = size;
		this.time = time;
	}
	
	public Dimension getSize() {
		return size;
	}
	
	public int getWidth() {
		return size.width;
	}
	
	public int getHeight() {
		return size.height;
	}
	
	public int getArea() {
		return getWidth() * getHeight();
	}
	
	public Module rotate() {
		return new Module(new Dimension(getHeight(), getWidth()), time);
	}
	
	public int getTime() {
		return time;
	}
	
	public void setTime(int time) {
		this.time = time;
	}
	
	@Override
	public String toString() {
		return getWidth() + "x" + getHeight() + " - " + time;
	}
	
	@Override
	public Module clone() {
		return new Module(size, time);
	}

}
