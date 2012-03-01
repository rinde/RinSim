package rinde.sim.scenario;

import java.util.Collection;
import java.util.LinkedList;

import com.google.common.base.Function;

public class ScenarioBuilder {
	
	private LinkedList<Generator<? extends TimedEvent>> generators;
	private Enum<?>[] supportedTypes;
	
	public boolean add(Generator<? extends TimedEvent> e) {
		return generators.add(e);
	}

	public Scenario build() {
		@SuppressWarnings("serial")
		Scenario s = new Scenario() {
			@Override
			public Enum<?>[] getPossibleEventTypes() {
				return supportedTypes;
			}
		};
		for (Generator<? extends TimedEvent> g : generators) {
			s.addAll(g.generate());
		}
		return s;
	}
	
	public ScenarioBuilder(Enum<?>... supportedTypes) {
		this.supportedTypes = supportedTypes;
		generators = new LinkedList<Generator<? extends TimedEvent>>();
	}
	
	public static interface Generator<T extends TimedEvent> {
		Collection<T> generate();
	}
	
	
	public static class MultipleEventGenerator<T extends TimedEvent> implements Generator<T> {
		
		private long time;
		private int amount;
		private Function<Long, T> function;


		public MultipleEventGenerator(long time, int amount,  Function<Long, T> function) {
			if(time < 0 || amount < 1) throw new IllegalArgumentException("incorrect parameters");
			this.time = time;
			this.amount = amount;
			this.function = function;
		}
		
		
		@Override
		public Collection<T> generate() {
			LinkedList<T> result = new LinkedList<T>();
			for(int i = 0; i < amount; ++i) {
				result.add(function.apply(time));
			}
			return result;
		}
		
	}
	
	public static class TimeSeries<T extends TimedEvent> implements Generator<T> {
		private long start;
		private long end;
		private long step;
		private Function<Long, T> function;
		
		public TimeSeries(long start, long end, long step, Function<Long, T> function) {
			if(start >= end || end < 0 || step < 1) throw new IllegalArgumentException("incorrect parameters");
			this.start = start;
			this.end = end;
			this.step = step;
			this.function = function;
		}
		
		public Collection<T> generate() {
			LinkedList<T> result = new LinkedList<T>();
			for(long t = start; t <= end; t += step) {
				result.add(function.apply(t));
			}
			return result;
		}
	}
	
	public static class EventTypeFunction implements Function<Long, TimedEvent>  {
		
		private Enum<?> typeEvent;

		public EventTypeFunction(Enum<?> type) {
			typeEvent = type;
		}

		@Override
		public TimedEvent apply(Long input) {
			return new TimedEvent(typeEvent, input);
		}
		
	}
}
