package dtu.compute.dmb.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;

import javax.swing.JPanel;

import dtu.compute.dmb.assay.ApplicationGraph;
import dtu.compute.dmb.assay.BioChip;
import dtu.compute.dmb.assay.ScheduleNode;

public class SchedulePanel extends JPanel {
	private static final long serialVersionUID = -8615640817860759587L;

	public static final int ROW_HEIGHT = 30;
	public static final int SPACING_HEIGHT = 10;
	private static final String SCHEDULE_TYPE_MIXING = "mixing";
	private static final String SCHEDULE_TYPE_STORAGE = "storage";
	private static final String SCHEDULE_TYPE_INPUT = "input";

	private Map<String, List<List<ScheduleNode>>> scheduleMonitor;
	private List<String> scheduleTypes;

	private int endTime;
	private int time;

	public SchedulePanel(BioChip chip, Collection<ScheduleNode> schedule) {
		PriorityQueue<ScheduleNode> copySchedule = schedule == null ? new PriorityQueue<>() : new PriorityQueue<>(schedule);
		endTime = ApplicationGraph.getEndTime(new ArrayList<>(copySchedule));
		endTime = endTime <= 0 ? 1 : endTime;
		time = 0;

		scheduleTypes = new ArrayList<>(Arrays.asList(new String[] {
				SCHEDULE_TYPE_MIXING,
				SCHEDULE_TYPE_STORAGE,
				SCHEDULE_TYPE_INPUT,
		}));

		scheduleMonitor = new HashMap<>();
		for (String type : scheduleTypes) {
			scheduleMonitor.put(type, new ArrayList<>());
		}
		while (!copySchedule.isEmpty()) {
			ScheduleNode node = copySchedule.poll();
			boolean placed = false;
			List<List<ScheduleNode>> temp;
			int area = node.getModule().getArea();
			if (area <= 3) {
				temp = scheduleMonitor.get(SCHEDULE_TYPE_INPUT);
			}
			else if (area <= 10) {
				temp = scheduleMonitor.get(SCHEDULE_TYPE_STORAGE);
			}
			else {
				temp = scheduleMonitor.get(SCHEDULE_TYPE_MIXING);
			}
			while (!placed) {
				for (List<ScheduleNode> scheduleRow : temp) {
					if (ApplicationGraph.getEndTime(scheduleRow) <= node.getStartTime() && (scheduleRow.isEmpty() ||
							scheduleRow.stream().anyMatch(x -> x.getAssayNode().getType().equals(node.getAssayNode().getType())))) {
						scheduleRow.add(node);
						placed = true;
						break;
					}
				}
				if (!placed) {
					temp.add(new ArrayList<>());
				}
			}
		}
		int rows = 0;
		for (String key : scheduleMonitor.keySet()) {
			rows += scheduleMonitor.get(key).size();
		}

		setPreferredSize(new Dimension(0, rows * ROW_HEIGHT + scheduleMonitor.keySet().size() * SPACING_HEIGHT + 20));
	}

	public void setTime(int time) {
		this.time = time;
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		g.setColor(getBackground().brighter());
		g.fillRect(0, 0, getWidth(), getHeight());
		
		g.setColor(getBackground().darker());
		int timeLines = endTime / 2000;
		for (int i = 1; i < timeLines; i++) {
			int x = i * getWidth() / timeLines;
			g.drawLine(x, 0, x, getHeight()-20);
		}

		int x = 0;
		int y = 0;
		for (String key : scheduleTypes) {
			for (List<ScheduleNode> row : scheduleMonitor.get(key)) {
				for (ScheduleNode node : row) {
					x = getWidth() * node.getStartTime() / endTime;
					int width = getWidth() * (node.getEndTime() - node.getStartTime()) / endTime;
					int height = ROW_HEIGHT;
					g.setColor(node.getColor());
					g.fillRect(x, y, width, height);
					g.setColor(Color.black);
					g.drawRect(x, y, width, height);
					g.clipRect(x, y, width, height);
					g.drawString(node.getAssayNode().getName(), x+10, y+height*2/3);
					g.setClip(null);
				}
				y += ROW_HEIGHT;
			}
			g.setColor(getBackground().darker().darker());
			g.fillRect(0, y, getWidth(), SPACING_HEIGHT);
			y += SPACING_HEIGHT;
		}

		g.setColor(Color.black);
		x = getWidth() * time / endTime;
		g.drawLine(x, 0, x, getHeight());
		
		double dTime = time/1000.;
		String timeString = String.format(Locale.UK, "Time: %05.2fs", dTime);
		g.drawString(timeString, 5, getHeight() - 5);
	}

}
