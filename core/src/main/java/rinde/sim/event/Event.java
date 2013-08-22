/**
 * 
 */
package rinde.sim.event;

import static com.google.common.base.Preconditions.checkState;

import java.io.Serializable;

import javax.annotation.Nullable;

/**
 * The base event class.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 */
public class Event implements Serializable {
    private static final long serialVersionUID = -390528892294335442L;

    /**
     * The type of event.
     */
    protected final Enum<?> eventType;
    @Nullable
    private transient Object issuer;

    /**
     * Create a new event instance.
     * @param type the event type.
     * @param pIssuer The event issuer, may be null.
     */
    public Event(Enum<?> type, @Nullable Object pIssuer) {
        eventType = type;
        issuer = pIssuer;
    }

    /**
     * Should be used only by extension classes when the issuer is not known at
     * creation time.
     * @param type The event type.
     */
    protected Event(Enum<?> type) {
        this(type, null);
    }

    /**
     * Issuer is set only if it was previously empty.
     * @param pIssuer The issuer of the event.
     */
    public void setIssuer(Object pIssuer) {
        checkState(issuer == null, "issuer is already set, can not be overridden. Value: %s.", issuer);
        issuer = pIssuer;
    }

    /**
     * @return The event issuer.
     */
    @Nullable
    public Object getIssuer() {
        return issuer;
    }

    /**
     * @return The type of event.
     */
    public Enum<?> getEventType() {
        return eventType;
    }

    @Override
    public String toString() {
        return "[Event " + eventType + "]";
    }

}
