package dtu.compute.dmb.assay;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class BioChip {

	private int[][] area;
	private Rectangle bounds;
	private Map<String, Point> inputBindings;
	private Set<Point> inputs;
	private Set<Point> outputs;
	private Map<Module, Point> modules;
	private List<Rectangle> maximalEmptyRectangles;
	private boolean dirty;

	public BioChip(int width, int height, Set<Point> inputs, Set<Point> outputs) {
		this.area = new int[width][height];
		this.bounds = new Rectangle(0, 0, width, height);
		this.inputs = new HashSet<>(inputs);
		this.outputs = new HashSet<>(outputs);
		this.inputBindings = new HashMap<>();
		this.modules = new HashMap<>();
		this.dirty = true;
	}

	public void removeAllModules() {
		modules = new HashMap<>();
		area = new int[getWidth()][getHeight()];
		dirty = true;
	}

	public boolean removeModule(Module module) {
		Point corner = modules.remove(module);
		if (corner != null) {
			for (int x = 0; x < module.getWidth(); x++) {
				for (int y = 0; y < module.getHeight(); y++) {
					area[corner.x+x][corner.y+y] = 0;
				}
			}
			dirty = true;
			return true;
		}
		return false;
	}

	public boolean addModule(Module module, Point placement) {
		boolean possible = true;
		for (int x = 0; x < module.getWidth(); x++) {
			for (int y = 0; y < module.getHeight(); y++) {
				if (area[placement.x+x][placement.y+y] > 0) {
					possible = false;
				}
			}
		}
		if (possible) {
			modules.put(module, placement);
			for (int x = 0; x < module.getWidth(); x++) {
				for (int y = 0; y < module.getHeight(); y++) {
					area[placement.x+x][placement.y+y] = modules.size();
				}
			}
			dirty = true;
		}
		return possible;
	}

	public Point addModule(Module module) {
		List<Rectangle> mers = getMaximalEmptyRectangles();
		Rectangle best = chooseBestRectangle(mers, module);
		if (best != null) {
			modules.put(module, best.getLocation());
			for (int x = 0; x < module.getWidth(); x++) {
				for (int y = 0; y < module.getHeight(); y++) {
					area[best.x+x][best.y+y] = modules.size();
				}
			}
			dirty = true;
		}
		return best == null ? null : best.getLocation();
	}
	
	private Rectangle chooseBestRectangle(List<Rectangle> rectangles, Module module) {
		Rectangle bestRect = null;
		int bestCost = Integer.MAX_VALUE;
		for (Rectangle rect : rectangles) {
			if (rect.width >= module.getWidth() && rect.height >= module.getHeight()) {
				int cost = Math.min(rect.width - module.getWidth(), rect.height - module.getHeight());
				if (cost < bestCost) {
					bestRect = rect;
					bestCost = cost;
				}
			}
		}
		return bestRect;
	}

	public boolean bindInput(AssayNode input) {
		if (!inputBindings.containsKey(input.getType())) {
			Set<Point> temp = fixLocations(inputs);
			temp.removeAll(inputBindings.values());
			Iterator<Point> i = temp.iterator();
			if (i.hasNext()) {
				Point next = i.next();
				inputBindings.put(input.getType(), next);
				return true;
			}
		}
		return false;
	}
	
	public void clearInputBindings() {
		inputBindings = new HashMap<>();
	}

	public Map<String, Point> getInputBindings() {
		return new HashMap<>(inputBindings);
	}

	public Point getInputLocations(AssayNode input) {
		Point location = inputBindings.get(input.getType());
		if (location == null) {
			if (bindInput(input)) {
				return getInputLocations(input);
			}
		}
		return location;
	}

	public Point getOutputLocation() {
		return getOutputs().iterator().next();
	}

	public List<Rectangle> getModulePlacement() {
		List<Rectangle> moduleRectangles = new ArrayList<>();
		for (Module module : modules.keySet()) {
			Point corner = modules.get(module);
			moduleRectangles.add(new Rectangle(corner, module.getSize()));
		}
		return moduleRectangles;
	}

	public Map<Module, Point> getModules() {
		return modules;
	}

	public int[][] getEmptyArea() {
		int[][] emptyArea = new int[getWidth()][getHeight()];

		List<Rectangle> rects = getMaximalEmptyRectangles();
		for (Rectangle rect : rects) {
			for (int x = 0; x < rect.width; x++) {
				for (int y = 0; y < rect.height; y++) {
					emptyArea[rect.x+x][rect.y+y]++;
				}
			}
		}
		return emptyArea;
	}
	
	public Set<Point> getInputs() {
		return fixLocations(inputs);
	}
	
	public Set<Point> getOutputs() {
		return fixLocations(outputs);
	}
	
	private Set<Point> fixLocations(Set<Point> locations) {
		Set<Point> fixed = new HashSet<>();
		for (Point point : locations) {
			int x = point.x;
			int y = point.y;
			if (x < 0) {
				x += getWidth();
			}
			if (y < 0) {
				y += getHeight();
			}
			fixed.add(new Point(x, y));
		}
			
		return fixed;
	}

	public List<Rectangle> getMaximalEmptyRectangles() {
		if (!dirty) {
			return maximalEmptyRectangles;
		}

		Module ioModule = new Module(new Dimension(1, 1), 0);
		Set<Module> ioModules = new HashSet<>();
		for (Point input : getInputs()) {
			Module module = ioModule.clone();
			addModule(module, input);
			ioModules.add(module);
		}
		for (Point output : getOutputs()) {
			Module module = ioModule.clone();
			addModule(module, output);
			ioModules.add(module);
		}

		int[][][] helperArea = new int[2][getWidth()][getHeight()];
		int count;
		for (int x = 0; x < getWidth(); x++) {
			count = 0;
			for (int y = 0; y < getHeight(); y++) {
				if (area[x][y] > 0) {
					count = 0;
				}
				else {
					helperArea[0][x][y] = ++count;
				}
			}
		}
		for (int y = 0; y < getHeight(); y++) {
			count = 0;
			for (int x = 0; x < getWidth(); x++) {
				if (area[x][y] > 0) {
					count = 0;
				}
				else {
					helperArea[1][x][y] = ++count;
				}
			}
		}

		Stack<Point> staircase = null;
		List<Rectangle> mers = new ArrayList<>();
		for (int x = 0; x < getWidth(); x++) {
			for (int y = 0; y < getHeight(); y++) {
				if (area[x][y] <= 0) {
					int h0 = helperArea[0][x][y];
					int h1 = helperArea[1][x][y];
					if (h0 <= 1) {
						staircase = new Stack<>();
						staircase.push(new Point(x - h1 + 1, y));
					}
					else if (h1 <= 1) {
						staircase = new Stack<>();
						staircase.push(new Point(x, y - h0 + 1));
					}
					else if (h1 < helperArea[1][x][y-1]) {
						int sy = staircase.isEmpty() ? y - h0 + 1 : staircase.peek().y;
						while (!staircase.isEmpty() && staircase.peek().x <= x - helperArea[1][x][y] + 1) {
							sy = staircase.isEmpty() ? y - h0 + 1 : staircase.peek().y;
							staircase.pop();
						}
						sy = staircase.isEmpty() ? y - h0 + 1 : sy;
						staircase.push(new Point(x - h1 + 1, sy));
					}
					else if (h1 > helperArea[1][x][y-1]) {
						staircase.push(new Point(x - h1 + 1, y));
					}

					if ((y+1 >= getHeight() || helperArea[1][x][y+1] < h1) &&
							(x+1 >= getWidth() || helperArea[0][x+1][y] < h0)) {
						for (Point p : staircase) {
							Rectangle rect = new Rectangle(p.x, p.y, x - p.x + 1, y - p.y + 1);
							if (y+1 >= getHeight() && x+1 >= getWidth()) {
								mers.add(rect);
							}
							else if (y+1 >= getHeight()) {
								if (p.y <= y - helperArea[0][x+1][y]) {
									mers.add(rect);
								}
							}
							else if (x+1 >= getWidth()) {
								if (p.x <= x - helperArea[1][x][y+1]) {
									mers.add(rect);
								}
							}
							else {
								if (p.x <= x - helperArea[1][x][y+1] && p.y <= y - helperArea[0][x+1][y]) {
									mers.add(rect);
								}
							}
						}
					}
				}
			}
		}
		maximalEmptyRectangles = mers;
		dirty = false;

		for (Module input : ioModules) {
			removeModule(input);
		}

		return mers;
	}

	public int getWidth() {
		return area.length;
	}

	public int getHeight() {
		return area[0].length;
	}

	public int getArea() {
		return getWidth() * getHeight();
	}

	public Rectangle getBounds() {
		return bounds;
	}

	public int[][] getPlacement() {
		return area;
	}

	public List<Module> getModuleLibrary() {
		List<Module> moduleLibrary = new ArrayList<>(Arrays.asList(
				new Module[] {new Module(new Dimension(7, 4), 4000), 
						new Module(new Dimension(6, 4), 6000),
						new Module(new Dimension(5, 3), 10000)}
				));
		for (int i = moduleLibrary.size() - 1; i >= 0; i--) {
			moduleLibrary.add(i, moduleLibrary.get(i).rotate());
		}
		return moduleLibrary;
	}

	public Module getInputModule() {
		return new Module(new Dimension(1, 1), 2000);
	}

	public Module getStorageModule() {
		return new Module(new Dimension(3, 3), 0);
	}

	public static void printArea(int[][] area) {
		int width = area.length;
		int height = area[0].length;
		printHorizontalLine(width);

		for (int y = height - 1; y >= 0; y--) {
			System.out.print("| ");
			for (int x = 0; x < width; x++) {
				System.out.print(String.format("%2d", area[x][y])+" ");
			}
			System.out.println("|");
		}

		printHorizontalLine(width);
	}

	private static void printHorizontalLine(int width) {
		for (int x = 0; x < width + 1; x++) {
			System.out.print("---");
		}
		System.out.println();
	}

	public void reset() {
		removeAllModules();
		this.inputBindings = new HashMap<>();
	}

}
