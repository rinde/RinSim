package rinde.sim.examples.rwalk5;

import rinde.sim.event.Event;

public class ServiceEndEvent extends Event {
  private static final long serialVersionUID = -2569785313757335609L;
  public final int pickedUp;
  public final int communicates;

  public ServiceEndEvent(int pickUps, int communicates, RandomWalkAgent truck) {
    super(RandomWalkAgent.Type.FINISHED_SERVICE, truck);
    this.pickedUp = pickUps;
    this.communicates = communicates;
  }
}
