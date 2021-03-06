/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.harvester.datamodel;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * This class implements a frequency of a number of weeks.
 */

public class WeeklyFrequency extends Frequency {

    /** The minute of the hour the event should happen at. */
    private int minute;
    /** The hour of the day the event should happen at. */
    private int hour;
    /** The day of the week the event should happen at. */
    private int dayOfWeek;

    /**
     * Create a new weekly frequency that happens every numUnits weeks, anytime.
     *
     * @param numUnits Number of weeks from event to event.
     * @throws ArgumentNotValid if numUnits if 0 or negative
     */
    public WeeklyFrequency(int numUnits) {
        super(numUnits, true);
    }

    /**
     * Create a new weekly frequency that happens every numUnits days, on the given day of week, hour, and minute.
     *
     * @param numUnits Number of days from event to event.
     * @param dayOfWeek Which day of the week this event should happen. Sunday is day 1 of the week.
     * @param hour The hour on which the event should happen.
     * @param minute The minute of hour on which the event should happen.
     * @throws ArgumentNotValid if numUnits if 0 or negative or dayOfWeek <1=SUNDAY >7=SATURDAY or hour is <0 or >23 or
     * minutes is <0 or >59
     */
    public WeeklyFrequency(int numUnits, int dayOfWeek, int hour, int minute) {
        super(numUnits, false);
        Calendar cal = GregorianCalendar.getInstance();
        if (dayOfWeek < cal.getMinimum(Calendar.DAY_OF_WEEK) || dayOfWeek > cal.getMaximum(Calendar.DAY_OF_WEEK)) {
            throw new ArgumentNotValid("Day in week must be in legal range '" + cal.getMinimum(Calendar.DAY_OF_WEEK)
                    + "' to '" + cal.getMaximum(Calendar.DAY_OF_WEEK) + "'");
        }
        if (hour < cal.getMinimum(Calendar.HOUR_OF_DAY) || hour > cal.getMaximum(Calendar.HOUR_OF_DAY)) {
            throw new ArgumentNotValid("Hour of day must be in legal range '" + cal.getMinimum(Calendar.HOUR_OF_DAY)
                    + "' to '" + cal.getMaximum(Calendar.HOUR_OF_DAY) + "'");
        }
        if (minute < cal.getMinimum(Calendar.MINUTE) || minute > cal.getMaximum(Calendar.MINUTE)) {
            throw new ArgumentNotValid("Minute must be in legal range '" + cal.getMinimum(Calendar.MINUTE) + "' to '"
                    + cal.getMaximum(Calendar.MINUTE) + "'");
        }

        this.dayOfWeek = dayOfWeek;
        this.hour = hour;
        this.minute = minute;
    }

    /**
     * Given when the last event happened, tell us when the next event should happen (even if the new event is in the
     * past).
     * <p>
     * The time of the next event is guaranteed to be later that lastEvent. For certain frequencies (e.g. once a day,
     * any time of day), the time of the next event is derived from lastEvent, for others (e.g. once a day at 13:00) the
     * time of the next event is the first matching time after lastEvent.
     *
     * @param lastEvent A time from which the next event should be calculated.
     * @return At what point the event should happen next.
     */
    public Date getNextEvent(Date lastEvent) {
        ArgumentNotValid.checkNotNull(lastEvent, "lastEvent");

        Calendar last = new GregorianCalendar();
        last.setTime(getFirstEvent(lastEvent));
        last.add(Calendar.WEEK_OF_YEAR, getNumUnits());
        return getFirstEvent(last.getTime());
    }

    /**
     * Given a starting time, tell us when the first event should happen.
     *
     * @param startTime The earliest time the event can happen.
     * @return At what point the event should happen the first time.
     */
    public Date getFirstEvent(Date startTime) {
        ArgumentNotValid.checkNotNull(startTime, "startTime");

        if (isAnytime()) {
            return startTime;
        }
        Calendar start = new GregorianCalendar();
        start.setTime(startTime);
        start.set(Calendar.MINUTE, minute);
        start.set(Calendar.HOUR_OF_DAY, hour);
        start.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        if (start.getTime().before(startTime)) {
            start.add(Calendar.WEEK_OF_YEAR, 1);
        }
        return start.getTime();

    }

    /**
     * If not anytime, the minute at which events should start.
     *
     * @return the minute
     */
    public int getMinute() {
        return minute;
    }

    /**
     * If not anytime, the hour at which events should start.
     *
     * @return the hour
     */
    public int getHour() {
        return hour;
    }

    /**
     * If not anytime, the day in the week at which events should start.
     *
     * @return the day. Sunday=1
     */
    public int getDayOfWeek() {
        return dayOfWeek;
    }

    /**
     * Autogenerated equals.
     *
     * @param o The object to compare with
     * @return Whether objects are equal
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WeeklyFrequency)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        final WeeklyFrequency weeklyFrequency = (WeeklyFrequency) o;

        if (isAnytime()) {
            return true;
        }

        if (dayOfWeek != weeklyFrequency.dayOfWeek) {
            return false;
        }
        if (hour != weeklyFrequency.hour) {
            return false;
        }
        if (minute != weeklyFrequency.minute) {
            return false;
        }

        return true;
    }

    /**
     * Autogenerated hashcode method.
     *
     * @return the hashcode
     */
    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + minute;
        result = 29 * result + hour;
        result = 29 * result + dayOfWeek;
        return result;
    }

    /**
     * Return the exact minute this event should happen on, or null if this is an anyTime event or doesn't define what
     * minute it should happen on.
     *
     * @return the exact minute this event should happen on
     */
    public Integer getOnMinute() {
        if (!isAnytime()) {
            return minute;
        }
        return null;
    }

    /**
     * Return the exact hour event should happen on, or null if this is an anyTime event or doesn't define what hour it
     * should happen on.
     *
     * @return the exact hour event should happen on
     */
    public Integer getOnHour() {
        if (!isAnytime()) {
            return hour;
        }
        return null;
    }

    /**
     * Return the exact day of week event should happen on, or null if this is an anyTime event or doesn't define what
     * day of week it should happen on.
     *
     * @return the exact day of week event should happen on
     */
    public Integer getOnDayOfWeek() {
        if (!isAnytime()) {
            return dayOfWeek;
        }
        return null;
    }

    /**
     * Return the exact day of month event should happen on, or null if this is an anyTime event or doesn't define what
     * day of month it should happen on.
     *
     * @return null (always)
     */
    public Integer getOnDayOfMonth() {
        return null;
    }

    /**
     * Return an integer that can be used to identify the kind of frequency. No two subclasses should use the same
     * integer
     *
     * @return an integer that can be used to identify the kind of frequency
     */
    public int ordinal() {
        return TimeUnit.WEEKLY.ordinal();
    }

    /**
     * Human readable representation of this object.
     *
     * @return Human readable representation
     */
    public String toString() {
        if (isAnytime()) {
            return "every " + getNumUnits() + " weeks";
        }
        return "every " + getNumUnits() + " weeks, on weekday " + dayOfWeek + " at " + hour + ":" + minute;
    }

}
