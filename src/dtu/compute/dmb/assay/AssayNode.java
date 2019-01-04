package dtu.compute.dmb.assay;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AssayNode {
	private static int ID_GEN = 0;

	private String name;
	private String type;
	private Color color;
	private int id;
	private int inputSize;
	private int outputSize;
	protected List<AssayNode> input;
	protected List<AssayNode> output;

	private AssayNode(String name, String type, int inputSize, int outputSize) {
		this.name = name;
		this.type = type;
		this.inputSize = inputSize;
		this.outputSize = outputSize;

		input = new ArrayList<>();
		output = new ArrayList<>();
		
		color = Color.gray;

		id = ID_GEN++;
	}

	public List<AssayNode> getInput() {
		return new ArrayList<AssayNode>(input);
	}

	public List<AssayNode> getOutput() {
		return new ArrayList<>(output);
	}
	
	public String getName() {
		String name = this.name;
		if (name.length() <= 0) {
			name = this.getClass().getSimpleName()+id+type;
		}
		return name;
	}

	public int getID() {
		return id;
	}
	
	public void setID(int id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}
	
	public Color getColor() {
		return color;
	}
	
	public void setColor(Color color) {
		this.color = color;
	}

	public void setOutput(int index, AssayNode node) {
		if (index < outputSize) {
			output.set(index, node);
		}
	}

	public void addOutput(AssayNode node) {
		if (output.size() < outputSize) {
			output.add(node);
		}
	}

	public void addInput(AssayNode node) {
		if (input.size() < inputSize) {
			input.add(node);
		}
	}
	
	public void removeInput(AssayNode node) {
		input.remove(node);
	}

	protected void generateOutput(AssayNode node) {
		input.add(node);
		if (input.size() >= inputSize) {
			for (AssayNode outputNode : output) {
				outputNode.generateOutput(this);
			}
		}
	}

	protected void clearInput() {
		input.clear();
		for (AssayNode outputNode : output) {
			outputNode.clearInput();
		}
	}

	protected boolean makeRoom(AssayNode old) {
		return output.remove(old);
	}

	protected void clearOutput() {
		output.clear();
	}

	public boolean isFull() {
		return output.size() >= outputSize;
	}

	public boolean isReady() {
		return input.size() >= inputSize;
	}

	protected Integer insertDepth(Map<AssayNode, Integer> map) {
		Integer depth = map.get(this);
		if (depth != null) {
			return depth;
		}
		depth = 0;
		for (AssayNode node : output) {
			depth += node.insertDepth(map);
		}
		depth++;
		map.put(this, depth);

		return depth;
	}
	
	public int prepare(Random rand, int id) {
		if (id > 0 && this.id > 0 || id < 0 && this.id < 0) return id;
		setID(id);
		setColor(new Color(100+rand.nextInt(156), 100+rand.nextInt(156), 100+rand.nextInt(156)));
		for (AssayNode input : getInput()) {
			if (input != null) {
				if (id > 0) {
					id++;
				}
				id = input.prepare(rand, id);
			}
		}
		return id;
	}

	@Override
	public String toString() {
		String string = getName();
		string += " (";
		for (AssayNode node : output) {
			string += node.getName() + "; ";
		}
		if (string.charAt(string.length()-1) == ' ') {
			string = string.substring(0, string.length()-2);
		}
		string += ")";
		return string;
	}

	public static class Source extends AssayNode {

		public Source() {
			super("Source", "", 0, Integer.MAX_VALUE);
		}

		public void generateOutput() {
			clearInput();
			generateOutput(null);
		}

		public Map<AssayNode, Integer> generateDepthMap() {
			Map<AssayNode, Integer> depthMap = new HashMap<>();
			insertDepth(depthMap);
			return depthMap;
		}
	}

	public static class Sink extends AssayNode {

		public Sink() {
			super("Sink", "", Integer.MAX_VALUE, 0);
		}

		@Override
		public List<AssayNode> getOutput() {
			return input;
		}
	}

	public static class Input extends AssayNode {

		public Input(String name, String type) {
			super(name, type, 1, 1);
		}
	}

	public static class Mix extends AssayNode {

		public Mix(String name) {
			super(name, "", 2, 2);
		}
	}

}
