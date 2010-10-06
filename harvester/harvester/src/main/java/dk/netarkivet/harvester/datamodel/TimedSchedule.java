/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package dk.netarkivet.harvester.datamodel;

import java.util.Date;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * This class implements a schedule that runs over a specified period of time.
 *
 */

public class TimedSchedule extends Schedule {
    /** The day this schedule should end.*/
    private final Date endDate;

    /** Create a new TimedSchedule that runs over a period of time.
     *
     * @param startDate The time at which the schedule starts running.  This
     * is not necessarily the time of the first event, but no events will
     * happen before this. May be null, meaning start any time.
     * @param endDate The time at which the schedule stops running.  No events
     * will happen after this. May be null, meaning continue forever.
     * @param frequency How frequently the event should happen.
     * @param comments Comments entered by the user
     * @param name The unique name of the schedule.
     * @throws ArgumentNotValid if frequency, name or comments is null, or name
     * is "" or
     */
    TimedSchedule(Date startDate, Date endDate, Frequency frequency,
                         String name, String comments) {
        super(startDate, frequency, name, comments);
        this.endDate = endDate;
    }

    /**
     * Autogenerated equals.
     * @param o The object to compare with
     * @return Whether objects are equal
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimedSchedule)) return false;
        if (!super.equals(o)) return false;

        final TimedSchedule timedSchedule = (TimedSchedule) o;

        if (endDate != null ? !endDate.equals(timedSchedule.endDate)
                : timedSchedule.endDate != null) return false;

        return true;
    }

    /**
     * Autogenerated hashcode method.
     * @return the hashcode
     */
    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + (endDate != null ? endDate.hashCode() : 0);
        return result;
    }

    /**
     * Return the date at which the next event will happen. If the calculated
     * next date is exactly equal to the end date then that value is
     * returned. If it is after the end date, null is returned.
     *
     * @param lastEvent The time at which the previous event happened.
     * If this is null, then the method returns null. Ie once one is after the
     * last event one is always after the last event.
     * @param numPreviousEvents How many events have previously happened
     * (ignored).
     * @return The date of the next event to happen or null for no more events.
     * @throws ArgumentNotValid if numPreviousEvents is negative
     */
    public Date getNextEvent(Date lastEvent, int numPreviousEvents) {
        ArgumentNotValid.checkNotNegative(numPreviousEvents,
                                          "numPreviousEvents");

        if (lastEvent == null) {
            return null;
        }
        Date nextEvent = frequency.getNextEvent(lastEvent);
        if (endDate == null) {
            return nextEvent;
        } else if (nextEvent.after(endDate)) {
            return null;
        } else {
            return nextEvent;
        }
    }

    /** Get the last possible time an event may be allowed.
     *
     * @return The last date, null means no last date, continue forever.
     */
    public Date getEndDate() {
        return endDate;
    }

    /** Human readable represenation of this object.
     *
     * @return Human readble representation
     */
    public String toString() {
        if (startDate == null && endDate == null) {
            return name + ": " + frequency + "(" + comments + ")";
        } else if (endDate == null) {
            return name + ": from " + startDate + " forever " + frequency
                   + "(" + comments + ")";
        } else if (startDate == null) {
            return name + ": until " + endDate.getTime() + " " + frequency
                   + "(" + comments + ")";
        } else {
            return name + ": from " + startDate + " to " + endDate.getTime()
                   + " " + frequency + "(" + comments + ")";
        }
    }
}