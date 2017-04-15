package org.openhab.binding.innogysmarthome.internal.listener;

public interface EventListener {

    /**
     * This method is called, whenever a new event comes from the innogy service (like a device change for example).
     *
     * @param msg
     */
    public void onEvent(String msg);

    /**
     * This method is called, whenever the eventRunner stops and must be restarted immediately.
     */
    public void onEventRunnerStopped();

    /**
     * This method is called, when the eventRunner stops and must be restarted after the given delay in seconds.
     * 
     * @param delay long in seconds
     */
    public void onEventRunnerStopped(long delay);
}
