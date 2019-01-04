package dtu.compute.dmb.io;

import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import dtu.compute.dmb.assay.Droplet;

public class AssayWriter extends FileWriter {

	public AssayWriter(File file) throws IOException {
		super(file);
	}

	public void writeSequence(List<Droplet> sequence, int timeStep) {
		int endTime = 0;
		for (Droplet droplet : sequence) {
			endTime = Math.max(endTime, droplet.getLatestTime());
		}
		try {
			String cmd = "setel";
			String line;
			int count;
			for (int time = 0; time <= endTime; time += timeStep) {
				line = cmd;
				count = 0;
				for (Droplet droplet : sequence) {
					Point p = droplet.getLocation(time);
					if (p != null) {
						if (count >= 10) {
							write(line+"\n");
							line = cmd;
							count = 0;
						}
						line += " " + pointToElectrode(p);
						count++;
					}
				}
				write(line+"\n");
				write("clra\n");
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private int pointToElectrode(Point p) {
		int electrode = 1;
		electrode += p.x % 4;
		electrode += p.x > 3 ? 4*16 : 0;
		if (p.x < 4) {
			electrode += p.y * 4;
		}
		else {
			electrode += 60 - p.y * 4;
		}
		return electrode;
	}

}
