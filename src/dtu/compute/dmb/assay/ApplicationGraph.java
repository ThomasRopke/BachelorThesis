package dtu.compute.dmb.assay;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import dtu.compute.dmb.assay.AssayNode.Input;
import dtu.compute.dmb.assay.AssayNode.Sink;
import dtu.compute.dmb.assay.AssayNode.Source;
import dtu.compute.dmb.assay.AssayNode.Mix;

public class ApplicationGraph {

	Map<String, AssayNode> nodes;
	Source source;
	Sink sink;

	public ApplicationGraph() {
		nodes = new HashMap<>();
		source = new Source();
		sink = new Sink();
	}

	public void addInput(Input input) {
		source.addOutput(input);
		input.addOutput(sink);
		nodes.put(input.getName(), input);
	}
	
	public Mix createMix(String name, String n1, String n2) {
		return createMix(name, nodes.get(n1), nodes.get(n2));
	}

	public Mix createMix(String name, AssayNode n1, AssayNode n2) {
		Set<AssayNode> output = generateOutput();
		Mix mix = null;
		if (output.contains(n1) && output.contains(n2)) {
			mix = new Mix(name);

			if (n1.makeRoom(sink)) {
				n1.addOutput(mix);
			}
			else {
				return mix;
			}

			if (n2.makeRoom(sink)) {
				n2.addOutput(mix);
			}
			else {
				return mix;
			}

			while (!mix.isFull()) {
				mix.addOutput(sink);
			}
		}
		nodes.put(mix.getName(), mix);
		return mix;
	}

	public Set<AssayNode> generateOutput() {
		source.generateOutput();
		return new HashSet<>(sink.getInput());
	}

	public void clearOutput() {
		source.clearInput();
	}
	
	public void prepare() {
		Random rand = new Random();
		generateOutput();
		sink.prepare(rand, -1);
		sink.prepare(rand, 1);
		clearOutput();
	}

	public PriorityQueue<ScheduleNode> createSchedule(BioChip chip) throws Exception {
		PriorityQueue<ScheduleNode> schedule = new PriorityQueue<>();
		PriorityQueue<Integer> timersQueue = new PriorityQueue<>();
		Set<AssayNode> inputs = new HashSet<>(source.output);
		clearOutput();
		chip.clearInputBindings();

		Map<AssayNode, Integer> depthMap = source.generateDepthMap();
		Comparator<AssayNode> operationComparator = new Comparator<AssayNode>() {
			@Override
			public int compare(AssayNode o1, AssayNode o2) {
				Integer d1 = depthMap.get(o1);
				d1 = d1 == null ? 0 : d1;
				int i1 = inputs.contains(o1) ? 0 : d1;
				Integer d2 = depthMap.get(o2);
				d2 = d2 == null ? 0 : d2;
				int i2 = inputs.contains(o2) ? 0 : d2;
				int diff = d2 + i2 - d1 - i1;
				diff = (diff == 0 ? o2.getID() - o1.getID() : diff);
				return diff;
			}
		};

		Map<AssayNode, Module> dummies = new HashMap<>();
		Set<AssayNode> initialNodes = new HashSet<>();
		for (AssayNode input : inputs) {
			dummies.put(input, chip.getStorageModule());
			input.addInput(source);
			for (AssayNode mix : input.getOutput()) {
				initialNodes.add(mix);
			}
		}
		PriorityQueue<AssayNode> operationQueue = new PriorityQueue<>(operationComparator);
		operationQueue.addAll(initialNodes);

		List<Module> moduleLibrary = chip.getModuleLibrary();
		List<ScheduleNode> storage = new ArrayList<>();

		int time = 0;
		while (!operationQueue.isEmpty() || !timersQueue.isEmpty()) {
			boolean full = false;
			while (!full && !operationQueue.isEmpty()) {
				AssayNode assayNode = operationQueue.peek();

				if (!assayNode.isReady()) {
					operationQueue.remove(assayNode);
					for (AssayNode input : inputs) {
						if (input.getOutput().contains(assayNode)) {
							operationQueue.add(input);
						}
					}
				}
				else {
					List<ScheduleNode> storageNodes = new ArrayList<>();
					full = true;
					Module module = null;
					Point placement = null;
					if (inputs.contains(assayNode)) {
						module = chip.getInputModule();
						placement = chip.getInputLocations(assayNode);
						if (placement != null && chip.addModule(module, placement)) {
							Module dummy = dummies.get(assayNode);
							Point dummyPlacement = chip.addModule(dummy);
							if (dummyPlacement != null) {
								storage.add(new ScheduleNode(0, null, dummy, dummyPlacement));
								full = false;
							}
							else {
								chip.removeModule(module);
							}
						}
					}
					else {
						for (ScheduleNode node : storage) {
							if (chip.removeModule(node.getModule())) {
								if (node.getAssayNode() != null) {
									node.getModule().setTime(time - node.getStartTime());
								}
								storageNodes.add(node);
							}
						}

						List<ScheduleNode> replacements = new ArrayList<>();
						for (Module libModule : moduleLibrary) {
							if (!full) break;
							module = libModule.clone();
							placement = chip.addModule(module);
							if (placement != null) {
								full = false;
								for (ScheduleNode node : storageNodes) {
									if (!assayNode.equals(node.getAssayNode())) {
										Module storageModule = node.getAssayNode() == null ? node.getModule() : chip.getStorageModule();
										Point storagePlacement = chip.addModule(storageModule);
										if (storagePlacement != null) {
											ScheduleNode storeNode = new ScheduleNode(time, node.getAssayNode(), storageModule, storagePlacement);
											replacements.add(storeNode);
										}
										else {
											for (ScheduleNode replacement : replacements) {
												chip.removeModule(replacement.getModule());
											}
											replacements.clear();
											full = true;
											break;
										}
									}
								}
								storage.addAll(replacements);
								if (full) {
									chip.removeModule(module);
								}
							}
						}
					}

					if (!full) {
						operationQueue.poll();
						ScheduleNode scheduleNode = new ScheduleNode(time, assayNode, module, placement);
						schedule.add(scheduleNode);
						timersQueue.add(time + module.getTime());
						inputs.remove(assayNode);
					}
					else {
						for (ScheduleNode node : storageNodes) {
							chip.addModule(node.getModule());
						}
					}
				}
			}

			if (timersQueue.isEmpty()) {
				throwNoSolution();
			}
			time = timersQueue.poll();
			while (!timersQueue.isEmpty() && timersQueue.peek() <= time) {
				timersQueue.poll();
			}

			for (ScheduleNode node : schedule) {
				if (node.getEndTime() == time) {
					chip.removeModule(node.getModule());
					chip.removeModule(dummies.get(node.getAssayNode()));
					AssayNode assayNode = node.getAssayNode();
					for (AssayNode output : assayNode.getOutput()) {
						output.addInput(assayNode);
						if (!output.equals(sink)) {
							Module storageModule = chip.getStorageModule();
							Point placement = chip.addModule(storageModule);
							if (placement == null) {
								throwNoSolution();
							}
							ScheduleNode storeNode = new ScheduleNode(time, output, storageModule, placement);
							storage.add(storeNode);
						}
						if (output.isReady()) {
							operationQueue.add(output);
						}
					}
				}
			}
		}
		schedule.addAll(storage.stream().filter(x -> x.getStartTime() < x.getEndTime()).collect(Collectors.toList()));
		chip.removeAllModules();
		
		PriorityQueue<ScheduleNode> copy = new PriorityQueue<>(schedule);
		List<ScheduleNode> active = new ArrayList<>();
		boolean freeInput = false;
		int reductionStep = chip.getInputModule().getTime();
		int reduction = 0;
		time = 0;
		while (!copy.isEmpty()) {
			ScheduleNode node = copy.poll();
			final int tempTime = node.getStartTime() - reduction;
			if (tempTime > time) {
				if (!active.stream().anyMatch(x -> x.getMixingArea() > 1) && freeInput) {
					reduction += reductionStep;
					active.stream().filter(x -> x.getModule().getArea() >= 9).forEach(x -> x.getModule().setTime(x.getModule().getTime()-reductionStep));
					active.stream().filter(x -> x.getModule().getArea() <= 1).forEach(x -> x.setStartTime(x.getStartTime()-reductionStep));
				}
				freeInput = !active.stream().anyMatch(x -> x.getModule().getArea() <= 1);
				active = active.stream().filter(x -> x.getEndTime() > tempTime).collect(Collectors.toList());
			}
			time = node.getStartTime() - reduction;
			node.setStartTime(time);
			active.add(node);
		}

		return schedule;
	}
	
	public static int getEndTime(Collection<ScheduleNode> schedule) {
		int endTime = 0;
		for (ScheduleNode node : schedule) {
			endTime = Math.max(endTime, node.getEndTime());
		}
		return endTime;
	}

	public List<Droplet> getSequence(PriorityQueue<ScheduleNode> schedule, BioChip chip, int timeStep) throws Exception {
		schedule = new PriorityQueue<>(schedule);
		int endTime = ApplicationGraph.getEndTime(schedule);
		
		source.generateOutput();
		List<Droplet> sequenceDroplets = new ArrayList<>();
		List<Droplet> readyDroplets = new ArrayList<>();
		List<Droplet> routingDroplets = new ArrayList<>();
		List<Droplet> mixingDroplets = new ArrayList<>();
		List<ScheduleNode> pendingNodes = new ArrayList<>();

		int time = 0;
		while ((!schedule.isEmpty() || !routingDroplets.isEmpty() || !mixingDroplets.isEmpty()) && time < endTime*10) {
			List<Droplet> storage = new ArrayList<>();
			while (!schedule.isEmpty() && schedule.peek().getStartTime() <= time) {
				ScheduleNode node = schedule.poll();
				pendingNodes.add(node);
				List<AssayNode> requirements = node.getAssayNode().getInput();
				List<Droplet> assigned = new ArrayList<>();

				for (AssayNode req : requirements) {
					if (node.getMixingArea() <= 1 && assigned.size() >= 1) {
						storage.addAll(assigned);
						break;
					}
					if (req.equals(source)) {
						Droplet droplet = new Droplet(node.getAssayNode());
						droplet.setInitialLocation(time + 3*timeStep, node.getCenter());
						droplet.setScheduleNode(node);

						sequenceDroplets.add(droplet);
						if (!droplet.generatePath(chip.getBounds(), sequenceDroplets, timeStep, false)) {
							throwNoSolution();
						}

						readyDroplets.add(droplet);
					}
					else {
						for (Droplet droplet : readyDroplets) {
							if (req.equals(droplet.getAssayNode()) && 
									droplet.getScheduleNode().getEndTime() <= time &&
									!storage.contains(droplet)) {
								assigned.add(droplet);
								droplet.setScheduleNode(node);
								if (!droplet.generatePath(chip.getBounds(), sequenceDroplets, timeStep, true)) {
									throwNoSolution();
								}
								break;
							}
						}
					}
				}
				if (node.getAssayNode().getInput().size() <= assigned.size()) {
					routingDroplets.addAll(assigned);
				}
				readyDroplets.removeAll(routingDroplets);
			}

			time += timeStep;

			List<Droplet> oldDroplet = new ArrayList<>();
			for (Droplet droplet : mixingDroplets) {
				if (droplet.getScheduleNode().getEndTime() <= time) {
					time += timeStep;
					int offset = 1;
					for (AssayNode split : droplet.getAssayNode().getOutput()) {
						Droplet splitDroplet = new Droplet(droplet.getAssayNode());
						Point initLocation = new Point(droplet.getLatestLocation());
						initLocation.translate(0, offset);
						splitDroplet.setInitialLocation(time, initLocation);
						offset *= -1;

						if (split.equals(sink)) {
							splitDroplet.setScheduleNode(new ScheduleNode(0, sink, new Module(new Dimension(), 0), chip.getOutputLocation()));
							if (!splitDroplet.generatePath(chip.getBounds(), sequenceDroplets, timeStep, true)) {
								throwNoSolution();
							}
						}
						else {
							splitDroplet.setScheduleNode(droplet.getScheduleNode());
							readyDroplets.add(splitDroplet);
						}
						sequenceDroplets.add(splitDroplet);
					}
					oldDroplet.add(droplet);
					time -= timeStep;
				}
			}
			mixingDroplets.removeAll(oldDroplet);

			List<ScheduleNode> oldSchedule = new ArrayList<>();
			for (ScheduleNode node : pendingNodes) {
				AssayNode assayNode = node.getAssayNode();
				if (node.getEndTime() <= time) {
					oldSchedule.add(node);
				}
				else {
					List<Droplet> ready = new ArrayList<>();
					for (Droplet droplet : routingDroplets) {
						if (droplet.isReady(time) && droplet.getScheduleNode().equals(node)) {
							ready.add(droplet);
						}
					}

					int requirements = assayNode.getInput().size();
					if (ready.size() >= requirements) {
						for (Droplet mergeDroplet : ready) {
							mergeDroplet.snipSequence(time);
						}
						Droplet droplet = new Droplet(assayNode);
						droplet.setInitialLocation(time, ready.get(0).getLatestLocation());
						droplet.setScheduleNode(node);
						if (!droplet.generatePath(chip.getBounds(), sequenceDroplets, timeStep, false)) {
							throwNoSolution();
						}
						sequenceDroplets.add(droplet);
						mixingDroplets.add(droplet);

						routingDroplets.removeAll(ready);
						oldSchedule.add(node);
					}
				}
			}
			pendingNodes.removeAll(oldSchedule);

		}
		return sequenceDroplets;
	}
	
	private void throwNoSolution() throws Exception {
		throw new Exception("No solution found!");
	}

	@Override
	public String toString() {
		return source.toString();
	}

}
