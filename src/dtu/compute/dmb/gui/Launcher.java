package dtu.compute.dmb.gui;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import dtu.compute.dmb.assay.ApplicationGraph;
import dtu.compute.dmb.assay.BioChip;
import dtu.compute.dmb.assay.Module;
import dtu.compute.dmb.io.AssayParser;

public class Launcher {
	
	public static void main(String[] args) {
		run();
	}
	
	public static void run() {
		ApplicationGraph graph = AssayParser.parseApplicationGraph(new File("graphs/fig12.1.graph"));

		BioChip chip = null;
		try {
			chip = AssayParser.parseBioChip(new File("chips/10x10.chip"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		final BioChip defaultChip = chip;
		EventQueue.invokeLater(new Runnable() {
		    public void run() {
		    	new AssayFrame(defaultChip, graph);
		    }
		} );
	}
	
	public static void placementTest() {
		Random rand = new Random();
		int width = 100;
		int height = 100;
		BioChip chip = new BioChip(width, height, new HashSet<>(), new HashSet<>());
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (rand.nextInt(5) < 1) {
					chip.addModule(new Module(new Dimension(1, 1), 0), new Point(x, y));
				}
			}
		}
		
		int[][] emptyArea = chip.getEmptyArea();
		BioChip.printArea(emptyArea);
		
		checkError(chip);
	}
	
	public static void checkError(BioChip chip) {
		List<Rectangle> l1 = chip.getMaximalEmptyRectangles();
		Set<Rectangle> set = new HashSet<Rectangle>(l1);
		if(set.size() < l1.size()){
		    System.err.println("Duplicate error");
		}
		
		for (Rectangle r1 : l1) {
			for (Rectangle r2 : l1) {
				if (r1.contains(r2) && !r1.equals(r2)) {
					System.out.println("Found non-mer rectangle");
				}
			}
		}

		int[][] ea = chip.getEmptyArea();
		for (int x = 0; x < chip.getWidth(); x++) {
			for (int y = 0; y < chip.getHeight(); y++) {
				if (chip.getPlacement()[x][y] > 0) {
					if (ea[x][y] > 0) {
						System.err.println("Empty rectangle on module");
					}
				}
				else {
					if (ea[x][y] <= 0) {
						System.err.println("Missing rectangle");
					}
				}
			}
		}
	}

}
