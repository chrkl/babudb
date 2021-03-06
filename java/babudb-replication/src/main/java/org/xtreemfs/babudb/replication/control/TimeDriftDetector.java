/*
 * Copyright (c) 2010 - 2011, Jan Stender, Bjoern Kolbeck, Mikael Hoegqvist,
 *                     Felix Hupfeld, Felix Langner, Zuse Institute Berlin
 * 
 * Licensed under the BSD License, see LICENSE file for details.
 * 
 */
package org.xtreemfs.babudb.replication.control;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.xtreemfs.babudb.config.ReplicationConfig;
import org.xtreemfs.babudb.replication.service.clients.ConditionClient;
import org.xtreemfs.foundation.LifeCycleListener;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 * Component that checks regularly the time-drift of all available replication
 * participants.
 * 
 * @author flangner
 * @since 03/30/2010
 */
 class TimeDriftDetector {
    
    /**
     * The maximum round trip time for a time().get() message between
     * this and another replica. If the round trip time of a
     * time().get() message exceeds this value, the message will be ignored.
     */
    private static final int MAX_RTT = 1000;
     
    /** listener to inform about an illegal time-drift */
    private final TimeDriftListener     listener;
    
    /** clients to check the time at */
    private final List<ConditionClient> participants;
    
    /** {@link Timer} to schedule the detection-task at */
    private final Timer                 timer;
    
    /** */
    private final int                   maxDrift;
    
    /** minimum delay between two checks */
    private final long                  DELAY_BETWEEN_CHECKS;
    
    /** listener to inform about lifeCycle events */
    private volatile LifeCycleListener  lifeCyclelistener       = null;
    
    /**
     * Initializes the detector. 
     * 
     * @param listener - to be informed, if an illegal time-drift was detected.
     * @param clients - to connect with the servers where to check for illegal 
     *                  drifts at.
     * @param client - to use to connect to the participants.
     * @param dMax - maximum time-drift allowed.
     */
    TimeDriftDetector(TimeDriftListener listener, List<ConditionClient> clients, int dMax) {
        
        this.listener = listener;
        this.participants = clients;
                    
        this.timer = new Timer("TimeDriftDetector", true);
        
        // Check at most every minute if the replica's clock are still in sync.
        this.DELAY_BETWEEN_CHECKS = Math.max(60000, participants.size() * ReplicationConfig.REQUEST_TIMEOUT);
        
        this.maxDrift = dMax;
    }
   
    /**
     * Sets a {@link LifeCycleListener} for this thread.
     * @param listener
     */
    void setLifeCycleListener(LifeCycleListener listener) {
        
        assert (listener != null);
        this.lifeCyclelistener = listener;
    }
    
    /**
     * Starts the timer and schedules the drift-check task.
     * May only be invoked once.
     */
    void start() {
        
        this.timer.schedule(new CheckTask(), 0L, DELAY_BETWEEN_CHECKS);
        if (this.lifeCyclelistener != null) {
            this.lifeCyclelistener.startupPerformed(); 
        }
    }

    /**
     * Terminates the timer. This could not be started again.
     */
    void shutdown() {
        
        this.timer.cancel();
        if (this.lifeCyclelistener != null) { 
            this.lifeCyclelistener.shutdownPerformed();
        }
    }
    
    /**
     * Listener to be informed about the detection of an illegal time-drift.
     * 
     * @author flangner
     * @since 03/30/2010
     */
    interface TimeDriftListener {
        
        /**
         * This method is executed if an illegal time-drift was detected.
         * 
         * @param humanReadableDriftedParticipants - a human readable string describing detected drifted participants.
         */
        void driftDetected(String humanReadableDriftedParticipants);
    }
    
    /**
     * Class for the check task performed by the TimeDriftDetector.
     * 
     * @author flangner
     * @since 03/30/2010
     */
    private final class CheckTask extends TimerTask {
        
        /*
         * (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            
            final Calendar calendar = Calendar.getInstance();
            final SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss:SSS yyyy zzz");
            
            String humanReadableDriftedParticipants = "";
            long start;
            long end;
            long cTime;
            int numDriftedClients = 0;
            
            for (ConditionClient client : participants) {
                
                try {
                    start = TimeSync.getGlobalTime();
                    cTime = client.time().get();
                    end = TimeSync.getGlobalTime();
                    
                    int rtt = (int)(end - start);
                    if (rtt > MAX_RTT) {
                        Logging.logMessage(Logging.LEVEL_INFO, Category.misc, this,
                                "Ignored time drift detection message since the probed replica (%s) took too long to respond (%d ms)",
                                client.getDefaultServerAddress().toString(),
                                rtt);
                        continue;
                    }
                    
                    final long estimatedRemoteTime = cTime + rtt / 2;
                    final long drift = Math.abs(end - estimatedRemoteTime);
                    if (drift > maxDrift) {
                        
                        calendar.setTimeInMillis(cTime);
                        String formattedCTime = format.format(calendar.getTime());
                        calendar.setTimeInMillis(start);
                        String formattedStartTime = format.format(calendar.getTime());
                        calendar.setTimeInMillis(end);
                        String formattedEndTime = format.format(calendar.getTime());
                        
                        humanReadableDriftedParticipants += "Saw a drift of at least " + drift + " ms as participant '" + 
                        client.getDefaultServerAddress().toString() + "' reported system time '" + formattedCTime + 
                        "' between local time '" + formattedStartTime + "' and '" +formattedEndTime + "'. ";
                        numDriftedClients++;
                        
                        // if the local time was overruled by at least 2 OR all other clients, the local time seems
                        // to be wrong
                        if (numDriftedClients > 1 || numDriftedClients == participants.size()) {
                            listener.driftDetected(humanReadableDriftedParticipants);
                            return;
                        }
                    }
                } catch (Throwable e) {
                    
                    Logging.logMessage(Logging.LEVEL_DEBUG, timer, 
                            "Local time of '%s' could not be fetched.", 
                            client.toString());
                } 
            }
        }
    }
}
