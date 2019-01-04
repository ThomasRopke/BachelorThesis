package dtu.compute.dmb.assay;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import dtu.compute.dmb.tools.AStar;

public class Droplet {

	private List<Entry> sequence;
	private ScheduleNode scheduleNode;
	private AssayNode assayNode;
	private Entry latestEntry;
	private Point direction;
	
	public static boolean isTouching(Point p1, Point p2) {
		return p1.distanceSq(p2) < 3;
	}

	public Droplet(AssayNode assayNode) {
		this.assayNode = assayNode;
		this.direction = new Point(1, 0);
	}
	
	public Color getColor() {
		return getAssayNode().getColor();
	}

	public ScheduleNode getScheduleNode() {
		return scheduleNode;
	}

	public void setScheduleNode(ScheduleNode scheduleNode) {
		this.scheduleNode = scheduleNode;
	}

	public AssayNode getAssayNode() {
		return assayNode;
	}
	
	private Entry getLatestEntry() {
		return latestEntry;
	}

	public Point getLatestLocation() {
		return latestEntry.getLocation();
	}
	
	public int getLatestTime() {
		return latestEntry.getTime();
	}

	public Point getTarget() {
		return scheduleNode.getCenter();
	}

	public boolean isReady() {
		return isReady(getLatestLocation());
	}
	
	public boolean isReady(int time) {
		return isReady(getLocation(time));
	}
	
	public boolean isReady(Point location) {
		return getTarget().equals(location);
	}
	
	public boolean isActive(int time) {
		int startTime = sequence.get(0).getTime();
		int endTime = sequence.get(sequence.size()-1).getTime();
		return startTime <= time && endTime >= time;
	}

	public Point getLocation(int time) {
		if (!isActive(time)) {
			return null;
		}
		for (Entry entry : sequence) {
			if (entry.getTime() >= time) {
				return entry.getLocation();
			}
		}
		return null;
	}
	
	public void setInitialLocation(int time, Point location) {
		sequence = new ArrayList<>();
		Entry init = new Entry(time, location);
		sequence.add(init);
		latestEntry = init;
	}
	
	public void snipSequence(int endTime) {
		final int time = Math.max(endTime, sequence.get(0).getTime());
		sequence = sequence.stream().filter(x -> x.getTime() <= time).collect(Collectors.toList());
		latestEntry = sequence.get(sequence.size() - 1);
	}
	
	public boolean generatePath(Rectangle bounds, List<Droplet> obstacles, int timeStep, boolean shouldRoute) {
		Map<Droplet, Integer> timeouts = new HashMap<>();
		return generatePath(timeouts, bounds, obstacles, timeStep, shouldRoute);
	}
	
	private boolean generatePath(Map<Droplet, Integer> timeouts, Rectangle bounds, List<Droplet> obstacles, int timeStep, boolean shouldRoute) {
		Entry entry = getLatestEntry();
		int time = entry.getTime() + timeStep;
		
		final int initTime = time;
		obstacles = obstacles.stream().filter(x -> x.getScheduleNode().getStartTime() < initTime).collect(Collectors.toList());
		
		if (shouldRoute) {
			DropletState init = new DropletState(this, entry.getLocation(), entry.getTime(), timeStep, bounds, obstacles);
			AStar<DropletState> pathfinder = new AStar<DropletState>(init);
			List<DropletState> path = pathfinder.search();
			if (path != null) {
				for (int i = 1; i < path.size(); i++) {
					DropletState state = path.get(i);
					latestEntry = new Entry(state.getTime(), state.getLocation());
					sequence.add(latestEntry);
				}
			}
		}
		
		time = getLatestEntry().getTime();
		Set<Droplet> collisions = new HashSet<>();
		while (time + timeStep <= getScheduleNode().getEndTime() ||
				getScheduleNode().getEndTime() <= 0 && !isReady()) {
			for (Droplet obstacle : obstacles) {
				Point location = obstacle.getLocation(time);
				if (location != null && Droplet.isTouching(location, getLatestLocation()) &&
						!obstacle.getTarget().equals(getTarget())) {
					collisions.add(obstacle);
				}
			}
			time += timeStep;
			addMove(time);
		}
		if (!collisions.isEmpty()) {
			obstacles.add(this);
			Integer count = timeouts.get(this);
			if (count == null) {
				timeouts.put(this, 1);
			}
			else {
				timeouts.put(this, ++count);
				if (count > 10) {
					return false;
				}
			}
		}
		for (Droplet collision : collisions) {
			collision.snipSequence(collision.getScheduleNode().getStartTime());
			if (!collision.generatePath(timeouts, bounds, obstacles, timeStep, true)) {
				return false;
			}
		}
		return true;
	}

	public void addMove(int time) {
		if (latestEntry == null) return;
		
		Point location = getLatestLocation();
		if (assayNode.equals(scheduleNode.getAssayNode())) {
			Point newLocation = null;
			boolean fits = false;
			if (getScheduleNode().getMixingArea() <= 1) {
				newLocation = location;
				fits = true;
			}
			while (!fits) {
				newLocation = new Point(location.x + direction.x, location.y + direction.y);
				fits = getScheduleNode().getMixingBounds().contains(newLocation);
				if (!fits) {
					direction.setLocation(-direction.y, direction.x);
				}
			}
			location = newLocation;
		}
		else {
			Point target = getTarget();
			Point diff = new Point(target.x - location.x, target.y - location.y);
			if (diff.x != 0 || diff.y != 0) {
				if (diff.x == 0) {
					location = new Point(location.x, location.y + (int)Math.signum(diff.y));
				}
				else if (diff.y == 0) {
					location = new Point(location.x + (int)Math.signum(diff.x), location.y);
				}
				else if (Math.abs(diff.x) > Math.abs(diff.y)) {
					location = new Point(location.x + (int)Math.signum(diff.x), location.y);
				}
				else {
					location = new Point(location.x, location.y + (int)Math.signum(diff.y));
				}
			}
		}
		latestEntry = new Entry(time, location);
		sequence.add(latestEntry);
	}

	@Override
	public String toString() {
		return assayNode.toString() + "\n" +
				scheduleNode.toString() + "\n" +
				sequence.toString();
	}
	
	private class Entry implements Comparable<Entry> {
		private int time;
		private Point location;
		
		public Entry(int time, Point location) {
			this.time = time;
			this.location = location;
		}
		
		public int getTime() {
			return time;
		}
		
		public Point getLocation() {
			return location;
		}

		@Override
		public int compareTo(Entry o) {
			return (int)Math.signum(time - o.time);
		}
		
		@Override
		public String toString() {
			return location.toString();
		}
	}

}
