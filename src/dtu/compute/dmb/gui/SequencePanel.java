package dtu.compute.dmb.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JPanel;

import dtu.compute.dmb.assay.BioChip;
import dtu.compute.dmb.assay.Droplet;
import dtu.compute.dmb.assay.ScheduleNode;

public class SequencePanel extends JPanel {
	private static final long serialVersionUID = -5381845919258961573L;
	
	private static final int CELL_SIZE = 30;
	
	private BioChip chip;
	private List<ScheduleNode> schedule;
	private List<Droplet> sequence;
	private int time;
	
	public SequencePanel(BioChip chip, Collection<ScheduleNode> schedule, Collection<Droplet> sequence) {
		this.chip = chip;
		this.schedule = schedule == null ? new ArrayList<>() : new ArrayList<>(schedule);
		this.sequence = sequence == null ? new ArrayList<>() : new ArrayList<>(sequence);
		
		setPreferredSize(new Dimension((chip.getWidth()+2)*CELL_SIZE, (chip.getHeight()+2)*CELL_SIZE));
	}
	
	public void setTime(int time) {
		this.time = time;
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		Color bgc = getBackground();
		g.setColor(bgc.darker());
		g.fillRect(0, 0, (chip.getWidth()+2)*CELL_SIZE, (chip.getHeight()+2)*CELL_SIZE);
		g.setColor(bgc);
		g.fillRect(CELL_SIZE, CELL_SIZE, chip.getWidth()*CELL_SIZE, chip.getHeight()*CELL_SIZE);
		
		List<Point> ioPoints = new ArrayList<>();
		ioPoints.addAll(chip.getInputBindings().values());
		ioPoints.add(chip.getOutputLocation());
		for (Point ioPoint : ioPoints) {
			int x = ioPoint.x;
			int y = ioPoint.y;
			int width = CELL_SIZE;
			int height = CELL_SIZE;
			if (x <= 0 || x >= chip.getWidth()-1) {
				width *= 3;
				x--;
			}
			else if (y <= 0 || y >= chip.getHeight()-1) {
				height *= 3;
				y--;
			}
			g.fillRect((x+1)*CELL_SIZE, (y+1)*CELL_SIZE, width, height);
		}
		
		for (ScheduleNode node : schedule) {
			if (node.getStartTime() <= time && node.getEndTime() >= time) {
				Rectangle rect = node.getBounds();
				
				g.setColor(node.getColor());
				g.fillRect((rect.x+1)*CELL_SIZE, (rect.y+1)*CELL_SIZE, rect.width*CELL_SIZE, rect.height*CELL_SIZE);
			}
		}
		
		g.setColor(Color.black);
		for (int x = 0; x <= chip.getWidth()+2; x++) {
			g.drawLine(x * CELL_SIZE, 0, x * CELL_SIZE, (chip.getHeight()+2)*CELL_SIZE);
		}
		for (int y = 0; y <= chip.getHeight()+2; y++) {
			g.drawLine(0, y * CELL_SIZE, (chip.getWidth()+2)*CELL_SIZE, y * CELL_SIZE);
		}
		
		for (Droplet droplet : sequence) {
			Point location = droplet.getLocation(time);
			if (location != null) {
				g.setColor(droplet.getColor());
				g.fillOval((location.x+1) * CELL_SIZE, (location.y+1) * CELL_SIZE, CELL_SIZE, CELL_SIZE);
				g.setColor(Color.black);
				g.drawOval((location.x+1) * CELL_SIZE, (location.y+1) * CELL_SIZE, CELL_SIZE, CELL_SIZE);
			}
		}
		
	}

}
