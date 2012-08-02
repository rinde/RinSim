package rinde.sim.problem.fabrirecht;

public class TimeWindow {
	public final long begin;
	public final long end;

	public TimeWindow(long pBegin, long pEnd) {
		begin = pBegin;
		end = pEnd;
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder("TimeWindow{");
		sb.append(begin);
		sb.append(",");
		sb.append(end);
		sb.append("}");
		return sb.toString();
	}
}