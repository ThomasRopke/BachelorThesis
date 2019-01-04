package dtu.compute.dmb.io;

import java.awt.Point;
import java.io.File;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import dtu.compute.dmb.assay.ApplicationGraph;
import dtu.compute.dmb.assay.AssayNode.Input;
import dtu.compute.dmb.assay.BioChip;

public class AssayParser {
	
	public static BioChip parseBioChip(File file) throws Exception {
		Scanner scanner = new Scanner(file);
		int width = scanner.nextInt();
		int height = scanner.nextInt();
		Set<Point> inputs = new HashSet<>();
		Set<Point> outputs = new HashSet<>();
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (line.length() <= 0) continue;
			line.replaceAll(" ", "");
			String[] content = line.split(":");
			String type = content[0];
			content = content[1].split(",");
			Point p = new Point(Integer.parseInt(content[0]), Integer.parseInt(content[1]));
			switch (type) {
			case "input":
				inputs.add(p);
				break;
			case "output":
				outputs.add(p);
				break;
			}
		}
		scanner.close();
		return new BioChip(width, height, inputs, outputs);
	}
	
	public static ApplicationGraph parseApplicationGraph(File file) {
		ApplicationGraph graph = new ApplicationGraph();
		try {
			parseApplicationGraph(graph, file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return graph;
	}
	
	public static void parseApplicationGraph(ApplicationGraph graph, File file) throws Exception {
		Scanner scanner = new Scanner(file);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (line.length() <= 0) continue;
			line.replaceAll(" ", "");
			String[] content = line.split(":");
			switch (content[0]) {
			case "input":
				graph.addInput(new Input(content[1], content[2]));
				break;
			case "mix":
				graph.createMix(content[1], content[2], content[3]);
				break;
			}
		}
		scanner.close();
	}

}
