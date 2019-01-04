package dtu.compute.dmb.gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import dtu.compute.dmb.assay.ApplicationGraph;
import dtu.compute.dmb.assay.BioChip;
import dtu.compute.dmb.assay.Droplet;
import dtu.compute.dmb.assay.ScheduleNode;
import dtu.compute.dmb.io.AssayParser;
import dtu.compute.dmb.io.AssayWriter;

public class AssayFrame extends JFrame implements KeyListener, MouseListener, MouseMotionListener {
	private static final long serialVersionUID = -5368039306269930097L;
	
	private static final int TIME_STEP = 50;
	
	private Timer timer;
	private int time;
	private int endTime;
	private int timeTurner;
	
	public AssayFrame(BioChip chip, ApplicationGraph graph) {
		
		graph.prepare();
		
		PriorityQueue<ScheduleNode> schedule = null;
		List<Droplet> sequence = null;
		timeTurner = 0;
		endTime = 0;
		time = 0;
		
		try {
			schedule = graph.createSchedule(chip);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (schedule != null) {
			endTime = ApplicationGraph.getEndTime(new ArrayList<>(schedule));
			try {
				sequence = graph.getSequence(schedule, chip, TIME_STEP);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		SequencePanel sePanel = new SequencePanel(chip, schedule, sequence);
		SchedulePanel scPanel = new SchedulePanel(chip, schedule);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(sePanel, BorderLayout.NORTH);
		getContentPane().add(scPanel, BorderLayout.SOUTH);
		
		addKeyListener(this);
		scPanel.addMouseListener(this);
		scPanel.addMouseMotionListener(this);
		
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				EventQueue.invokeLater(new Runnable() {
				    public void run() {
				    	time = Math.max(0, time + TIME_STEP * timeTurner);
						sePanel.setTime(time);
						scPanel.setTime(time);
						repaint();
				    }
				});
			}
		}, 0, TIME_STEP);
		
		setupMenu(chip, graph, sequence);
		
		setDefaultCloseOperation(AssayFrame.EXIT_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
		getGraphics().drawString("load", 0, 0);
		setVisible(true);
	}
	
	private void setTime(MouseEvent e) {
		JPanel panel = (JPanel) e.getSource();
		float time = endTime * e.getX() / panel.getWidth();
		time /= TIME_STEP;
		time = Math.round(time);
		time *= TIME_STEP;
		this.time = (int)time;
	}
	
	private void setupMenu(BioChip chip, ApplicationGraph graph, List<Droplet> sequence) {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("Menu");
		
		JMenuItem exportItem = new JMenuItem("Export");
		JMenuItem chipItem = new JMenuItem("Change Chip");
		JMenuItem graphItem = new JMenuItem("Change Graph");
		
		exportItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				String path = JOptionPane.showInputDialog(AssayFrame.this, "File name:", "Export", JOptionPane.PLAIN_MESSAGE);
				if (path == null) return;
				try {
					AssayWriter writer = new AssayWriter(new File(path));
					writer.writeSequence(sequence, TIME_STEP);
					writer.close();
					JOptionPane.showMessageDialog(AssayFrame.this, "Succesfully exported sequence to \""+path+"\"", "Succes", JOptionPane.PLAIN_MESSAGE);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		
		chipItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				String path = JOptionPane.showInputDialog(AssayFrame.this, "File name:", "Change Chip", JOptionPane.PLAIN_MESSAGE);
				if (path == null) return;
				try {
					BioChip newChip = AssayParser.parseBioChip(new File(path));
					EventQueue.invokeLater(new Runnable() {
					    public void run() {
					    	AssayFrame.this.dispose();
					    	new AssayFrame(newChip, graph);
					    }
					} );
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		graphItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				String path = JOptionPane.showInputDialog(AssayFrame.this, "File name:", "Change Graph", JOptionPane.PLAIN_MESSAGE);
				if (path == null) return;
				try {
					ApplicationGraph newGraph = AssayParser.parseApplicationGraph(new File(path));
					chip.reset();
					EventQueue.invokeLater(new Runnable() {
					    public void run() {
					    	AssayFrame.this.dispose();
					    	new AssayFrame(chip, newGraph);
					    }
					} );
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		menuBar.add(menu);
		menu.add(chipItem);
		menu.add(graphItem);
		menu.add(exportItem);
		setJMenuBar(menuBar);
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_LEFT:
			timeTurner--;
			break;
		case KeyEvent.VK_RIGHT:
			timeTurner++;
			break;
		}
		timeTurner = (int)Math.signum(timeTurner);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_LEFT:
			timeTurner++;
			break;
		case KeyEvent.VK_RIGHT:
			timeTurner--;
			break;
		}
		timeTurner = (int)Math.signum(timeTurner);
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		setTime(e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mouseDragged(MouseEvent e) {
		setTime(e);
	}

	@Override
	public void mouseMoved(MouseEvent e) {}

}
