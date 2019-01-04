// Original Author: Shivanker Goel

package dtu.compute.dmb.tools;

import java.util.*;

public class AStar<S extends State> {

	private class PrioState implements Comparable<PrioState>	{
		S state;
		int prio;
		PrioState(S s,int prio)	{
			state = s;
			this.prio = prio;
		}
		public int compareTo(PrioState c)	{
			return prio - c.prio;				// lower prio => higher priority
		}
		public boolean equals(Object o)	{
			@SuppressWarnings("unchecked")
			PrioState p = (PrioState) o;
			return state.toString().equals(p.state.toString());
		}
	}

	private S init;
	private PriorityQueue<PrioState> fringe = new PriorityQueue<PrioState>();
	private Map<String, LinkedList<S>> paths = new HashMap<String, LinkedList<S>>();

	public AStar(S init)	{
		this.init = init;
	}

	public LinkedList<S> search()	{
		fringe.add(new PrioState(init, init.heuristic()));
		paths.put(init.toString(), new LinkedList<S>());
		while(!fringe.isEmpty())	{
			PrioState c = fringe.remove();
			if(c.state.isGoal())	{
				LinkedList<S> p = paths.get(c.state.toString());
				p.add(c.state);
				return p;
			}

			@SuppressWarnings("unchecked")
			S[] succs = (S[])c.state.successors();
			for(S succ : succs)	{
				LinkedList<S> p = paths.get(succ.toString());
				if( p == null || p.size() - 1 > paths.get(c.state.toString()).size())	{
					if(p != null)	{
						fringe.remove(new PrioState(succ, p.size() + succ.heuristic()));
						p.clear();
					}
					else
						p = new LinkedList<S>();
					Iterator<S> op = paths.get(c.state.toString()).iterator();
					while(op.hasNext())
						p.add(op.next());
					p.add(c.state);
					paths.put(succ.toString(), p);
					fringe.add(new PrioState(succ, p.size() + succ.heuristic()));
				}
			}
		}
		return null;
	}
}