/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.units;

import java.util.Date;
import java.text.ParseException;

/**
 * Implements a range of dates, using DateType and/or TimeDuration.
 * You can use a DateType = "present" and a time duration to specify "real time" intervals, eg
 *   "last 3 days" uses endDate = "present" and duration = "3 days".
 *
 * @author john caron
 */

public class DateRange {
  private DateType start, end;
  private TimeDuration duration, resolution;
  private boolean isEmpty, useStart, useEnd, useDuration, useResolution;

  /** default Constructor
   * @throws java.text.ParseException artifact, cant happen
   */
  public DateRange() throws ParseException {
      this( null, new DateType(false, new Date()), new TimeDuration("1 day"), new TimeDuration("15 min"));
  }

  /**
   * Create Date Range from a start and end date
   * @param start start of range
   * @param end end of range
   */
  public DateRange(Date start, Date end) {
    this( new DateType(false, start), new DateType(false, end), null, null);
  }

  /**
   * Create DateRange from another DateRange, with a different units of resolution.
   * @param range copy start and end from here
   * @param timeUnits make resolution using new TimeDuration( timeUnits)
   * @throws Exception is units are not valid time units
   */
  public DateRange(DateRange range, String timeUnits) throws Exception {
    this( new DateType(false, range.getStart().getDate()), new DateType(false, range.getEnd().getDate()), null, new TimeDuration( timeUnits));
  }

  /**
   * Encapsolates a range of dates, using DateType start/end, and/or a TimeDuration.
   * A DateRange can be specified in any of the following ways:
    <ol>
     <li> a start date and end date
     <li> a start date and duration
     <li> an end date and duration
    </ol>
   *
   * @param start starting date
   * @param end ending date
   * @param duration time duration
   * @param resolution time resolution; optional
   */
  public DateRange(DateType start, DateType end, TimeDuration duration, TimeDuration resolution) {
    this.start = start;
    this.end = end;
    this.duration = duration;
    this.resolution = resolution;

    useStart = (start != null) && !start.isBlank();
    useEnd = (end != null) && !end.isBlank();
    useDuration = (duration != null);
    useResolution = (resolution != null);

    boolean invalid = true;
    if (useStart && useEnd) {
      invalid = false;
      recalcDuration();

    } else if (useStart && useDuration) {
      invalid = false;
      this.end = this.start.add( duration);

    } else if (useEnd && useDuration) {
      invalid = false;
      this.start = this.end.subtract( duration);
    }
    if (invalid)
      throw new IllegalArgumentException("DateRange must have 2 of start, end, duration");

    checkIfEmpty();
    hashCode = 0;
  }

  private void checkIfEmpty() {
    isEmpty = this.end.before( this.start);
    if (isEmpty)
      duration.setValueInSeconds(0);
  }

    // choose a resolution based on # seconds
  private String chooseResolution(double time) {
    if (time < 180) // 3 minutes
      return "secs";
    time /= 60; // minutes
    if (time < 180) // 3 hours
      return "minutes";
    time /= 60; // hours
    if (time < 72) // 3 days
      return "hours";
    time /= 24; // days
    if (time < 90) // 3 months
      return "days";
    time /= 30; // months
    if (time < 36) // 3 years
      return "months";
    return "years";
  }

  private void recalcDuration() {
    long min = getStart().getDate().getTime();
    long max = getEnd().getDate().getTime();
    double secs = .001 * (max - min);
    if (secs < 0)
      secs = 0;

    if (duration == null) {
      try {
        duration = new TimeDuration( chooseResolution(secs));
      } catch (ParseException e) {
        // cant happen
      }
    }

    if (resolution == null) {
      duration.setValueInSeconds( secs);
    } else {
       // make it a multiple of resolution
      double resSecs = resolution.getValueInSeconds();
      double closest = Math.round(secs / resSecs);
      secs = closest * resSecs;
      duration.setValueInSeconds( secs);
    }

    hashCode = 0;
  }

  /**
   * Determine if the given date is included in this date range.
   * The date range includes the start and end dates.
   * @param d date to check
   * @return  true if date in inside this range
   */
  public boolean included( Date d) {
    if (isEmpty) return false;

    if (start.after( d)) return false;
    if (end.before( d)) return false;

    return true;
  }

  /**
   * Determine if the given range intersects this date range.
   * @param start_want  range starts here
   * @param end_want  range ends here
   * @return  true if ranges intersect
   */
  public boolean intersects( Date start_want, Date end_want) {
    if (isEmpty) return false;

    if (start.after( end_want)) return false;
    if (end.before( start_want)) return false;

    return true;
  }

  /**
   * Intersect with another date range
   * @param clip interset with this date range
   * @return new date range that is the intersection
   */
  public DateRange intersect( DateRange clip) {
    if (isEmpty) return this;
    if (clip.isEmpty) return clip;

    DateType s = start.before( clip.getStart()) ? clip.getStart() : start;
    DateType e = end.before( clip.getEnd()) ? end : clip.getEnd();

    return new DateRange(s, e, null, resolution);
  }

  /** Extend this date range by the given one.
   * @param dr given DateRange
   **/
  public void extend( DateRange dr) {
    boolean localEmpty = isEmpty;
    if (localEmpty || dr.getStart().before( start))
      setStart( dr.getStart());
    if (localEmpty || end.before(dr.getEnd()))
      setEnd( dr.getEnd());
  }

  public DateType getStart() { return start; }
  public void setStart(DateType start) {
    this.start = start;
    useStart = true;

    if (start.isPresent()) {
      this.end = start.add( duration);
      useEnd = false;
    } else if (end.isPresent()) {
      recalcDuration();
      this.start = end.subtract( duration);
    } else {
      recalcDuration();
      this.end = start.add( duration);
    }
    checkIfEmpty();
  }

  public DateType getEnd() { return end; }
  public void setEnd(DateType end) {
    this.end = end;
    useEnd = true;

    if (end.isPresent()) {
      this.start = end.subtract( duration);
      useStart = false;
    } else if (start.isPresent()) {
      recalcDuration();
      this.end = start.add( duration);
    } else {
      recalcDuration();
      this.start = end.subtract( duration);
    }
    checkIfEmpty();
  }

  public TimeDuration getDuration() { return duration; }
  public void setDuration(TimeDuration duration) {
    this.duration = duration;
    useDuration = true;

    if (this.end.isPresent()) {
      this.start = end.subtract( duration);
      useStart = false;
    } else {
      this.end = start.add( duration);
      useEnd = false;
    }
    checkIfEmpty();
  }

  public TimeDuration getResolution() { return resolution; }
  public void setResolution(TimeDuration resolution) {
    this.resolution = resolution;
    useResolution = true;
  }

  public boolean useStart() { return useStart; }
  public boolean useEnd() { return useEnd; }
  public boolean useDuration() { return useDuration; }
  public boolean useResolution() { return useResolution; }

  /**
   * Return true if start date equals end date, so date range is a point.
   * @return true if start = end
   */
  public boolean isPoint() {
    return !isEmpty && start.equals( end);
  }

  /**
   * If the range is empty
   * @return if the range is empty
   */
  public boolean isEmpty() {
    return isEmpty;
  }

  public String toString() { return "start= "+start +" end= "+end+ " duration= "+ duration
        + " resolution= "+ resolution; }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DateRange)) return false;
    DateRange oo = (DateRange) o;
    if (useStart && !start.equals(oo.start)) return false;
    if (useEnd && !end.equals(oo.end)) return false;
    if (useDuration && !duration.equals(oo.duration)) return false;
    if (useResolution && !resolution.equals(oo.resolution)) return false;
    return true;
  }

 /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      if ( useStart )
        result = 37*result + start.hashCode();
      if ( useEnd )
        result = 37*result + end.hashCode();
      if ( useDuration )
        result = 37*result + duration.hashCode();
      if ( useResolution )
        result = 37*result + resolution.hashCode();
      hashCode = result;
    }
    return hashCode;
  }
  private int hashCode = 0; // Bloch, item 8

}