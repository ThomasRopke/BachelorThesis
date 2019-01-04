// Original Author: Shivanker Goel

package dtu.compute.dmb.tools;

public abstract class State	{
	protected abstract boolean isGoal();
	protected abstract Object[] successors();
	protected abstract int heuristic();	// heuristic <= min. no. of steps needed to reach the goal; the closer, the better
	abstract public String toString();	// for proper hashCode and equality check in HashMap
}
