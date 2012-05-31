package rinde.sim.scenario;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;
import java.util.LinkedList;

import com.google.common.base.Function;

public class ScenarioBuilder {

	private final LinkedList<Generator<? extends TimedEvent>> generators;
	private final Enum<?>[] supportedTypes;

	public ScenarioBuilder(Enum<?>... pSupportedTypes) {
		checkArgument(pSupportedTypes != null, "supported types can not be null");
		supportedTypes = pSupportedTypes;
		generators = new LinkedList<Generator<? extends TimedEvent>>();
	}

	public boolean add(Generator<? extends TimedEvent> generator) {
		checkArgument(generator != null, "generator can not be null");
		return generators.add(generator);
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

	public static interface Generator<T extends TimedEvent> {
		Collection<T> generate();
	}

	public static class MultipleEventGenerator<T extends TimedEvent> implements Generator<T> {

		private final long time;
		private final int amount;
		private final Function<Long, T> function;

		public MultipleEventGenerator(long pTime, int pAmount, Function<Long, T> pFunction) {
			checkArgument(pTime >= 0, "time can not be negative");
			checkArgument(pAmount >= 1, "amount must be at least 1");
			checkArgument(pFunction != null, "function can not be null");
			this.time = pTime;
			this.amount = pAmount;
			this.function = pFunction;
		}

		@Override
		public Collection<T> generate() {
			LinkedList<T> result = new LinkedList<T>();
			for (int i = 0; i < amount; ++i) {
				result.add(function.apply(time));
			}
			return result;
		}

	}

	public static class TimeSeries<T extends TimedEvent> implements Generator<T> {
		private final long start;
		private final long end;
		private final long step;
		private final Function<Long, T> function;

		public TimeSeries(long pStart, long pEnd, long pStep, Function<Long, T> pFunction) {
			checkArgument(pStart < pEnd, "start time must be before end");
			checkArgument(pEnd > 0, "end time must be greather than 0");
			checkArgument(pStep >= 1, "time step must be >= than 1");
			checkArgument(pFunction != null, "function can not be null");
			this.start = pStart;
			this.end = pEnd;
			this.step = pStep;
			this.function = pFunction;
		}

		@Override
		public Collection<T> generate() {
			LinkedList<T> result = new LinkedList<T>();
			for (long t = start; t <= end; t += step) {
				result.add(function.apply(t));
			}
			return result;
		}
	}

	public static class EventTypeFunction implements Function<Long, TimedEvent> {

		private final Enum<?> typeEvent;

		public EventTypeFunction(Enum<?> type) {
			checkArgument(type != null);
			typeEvent = type;
		}

		@Override
		public TimedEvent apply(Long input) {
			return new TimedEvent(typeEvent, input);
		}

	}
}
