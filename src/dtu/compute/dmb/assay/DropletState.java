package dtu.compute.dmb.assay;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dtu.compute.dmb.tools.State;

public class DropletState extends State {
	private List<Droplet> obstacles;
	private Droplet droplet;
	private Point location;
	private int time;
	private int timeStep;
	private Rectangle bounds;
	
	public DropletState(Droplet droplet, Point location, int time, int timeStep, Rectangle bounds, List<Droplet> obstacles) {
		this.droplet = droplet;
		this.location = location;
		this.time = time;
		this.timeStep = timeStep;
		this.bounds = bounds;
		this.obstacles = obstacles;
	}
	
	public DropletState(DropletState previous, Point location) {
		this(previous.droplet, location, 
				previous.time + previous.timeStep, 
				previous.timeStep, previous.bounds, 
				previous.obstacles);

	}
	
	public Point getLocation() {
		return location;
	}
	
	public int getTime() {
		return time;
	}

	@Override
	protected boolean isGoal() {
		return droplet.isReady(location);
	}

	@Override
	protected Object[] successors() {
		DropletState[] moves = new DropletState[] {
				new DropletState(this, new Point(location.x, location.y)),
				new DropletState(this, new Point(location.x+1, location.y)),
				new DropletState(this, new Point(location.x-1, location.y)),
				new DropletState(this, new Point(location.x, location.y+1)),
				new DropletState(this, new Point(location.x, location.y-1)),
		};
		
		Map<Point, Droplet> realObstacles = new HashMap<>();
		for (Droplet obstacle : obstacles) {
			for (int i = 0; i <= 2; i++) {
				Point location = obstacle.getLocation(time + i*timeStep);
				if (location != null && !obstacle.equals(droplet)) {
					realObstacles.put(location, obstacle);
				}
			}
		}
		
		List<DropletState> successors = new ArrayList<>();
		for (DropletState state : moves) {
			if (!bounds.contains(state.getLocation())) continue;
			if (realObstacles.keySet().stream().anyMatch(x -> Droplet.isTouching(x, state.getLocation()) &&
					(!realObstacles.get(x).getTarget().equals(droplet.getTarget()) || !x.equals(droplet.getTarget())))) continue;
			successors.add(state);
		}
		return successors.toArray(new DropletState[0]);
	}

	@Override
	protected int heuristic() {
		Point target = droplet.getTarget();
		return Math.abs(target.x-location.x) + Math.abs(target.y-location.y);
	}

	@Override
	public String toString() {
		return location.toString() + time;
	}

}
