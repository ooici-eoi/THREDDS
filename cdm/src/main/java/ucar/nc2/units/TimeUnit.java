// $Id: TimeUnit.java 63 2006-07-12 21:50:51Z edavis $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

import ucar.units.ConversionException;
import ucar.unidata.util.Format;

import java.util.StringTokenizer;
import java.util.Date;
import java.util.Calendar;

/**
 * Handles Units that are time durations, eg in seconds, hours, days, years.
 * It keeps track of the original unit name, rather than converting to canonical "seconds".
 * The unit name never changes, but the value may.
 * <p>
 * This is a wrapper around ucar.units.
 * The underlying ucar.units.Unit always has a value of "1.0", ie is a base unit.
 *
 * @author John Caron
 * @version $Revision: 63 $ $Date: 2006-07-12 15:50:51 -0600 (Wed, 12 Jul 2006) $
 */
public class TimeUnit extends SimpleUnit {
  private double value;
  private double factor = 1.0;
  private String unitString;

  /**
   * Constructor from a String.
   * @param text [value] <time unit> eg "hours" or "13 hours". Time unit is from udunits.
   * @throws Exception is bad format
   */
  public TimeUnit(String text) throws Exception {
    super();

    if (text == null) {
      this.value = 1.0;
      this.unitString = "secs";
      this.uu = SimpleUnit.makeUnit( unitString); // always a base unit
      return;
    }

    StringTokenizer stoker = new StringTokenizer(text);
    int ntoke = stoker.countTokens();
    if (ntoke == 1) {
      this.value = 1.0;
      this.unitString = stoker.nextToken();

    } else if (ntoke == 2){
      this.value = Double.parseDouble(stoker.nextToken());
      this.unitString = stoker.nextToken();
    } else
      throw new IllegalArgumentException("Not TimeUnit = "+text);

    uu = SimpleUnit.makeUnit( unitString); // always a base unit
    factor = uu.convertTo( 1.0, secsUnit);
  }

  /**
   * Constructor from a value and a unit name.
   * @param value amount of the unit.
   * @param unitString  Time unit string from udunits.
   * @throws Exception
   */
  public TimeUnit(double value, String unitString) throws Exception {
    this.value = value;
    this.unitString = unitString;
    uu = SimpleUnit.makeUnit( unitString);
    factor = uu.convertTo( 1.0, secsUnit);
  }

  /**
   * Copy Constructor.
   */
  public TimeUnit( TimeUnit src) {
    this.value = src.getValue();
    this.unitString = src.getUnitString();
    uu = src.getUnit();
    factor = src.getFactor();
  }

  /** Get the value. */
  public double getValue() { return value; }

  /** Get the factor that converts this value to seconds.
   * getValueInSeconds = factor * value
   */
  public double getFactor() { return factor; }

  /** Set the value in the original units. */
  public void setValue( double value) {
    this.value = value;
  }

  /** Get the "base" unit String, eg "secs" or "days" */
  public String getUnitString() { return unitString; }

  /** String representation. */
  public String toString() {
    return Format.d(value, 5) + " "+unitString;
  }

  /** Get the time duration in seconds. */
  public double getValueInSeconds() {
    return factor * value;
  }

  /** Get the time duration in seconds of the specified value. */
  public double getValueInSeconds(double value) {
    return factor * value;
  }

  /**
   * Set the value, using the given number of seconds.
   * @param secs : number of seconds; convert this to the units of this TimeUnit.
   */
  public void setValueInSeconds( double secs) {
    value = secs/factor;
  }

  // override

  /** Convert given value of this unit to the new unit.
   *  <em>NOTE: the current value of this unit ignored, the given value is used instead.
   * This is different than ucar.units or SimpleUnit.</em>
   * @param value in the units of this "base unit"
   * @param outputUnit convert to this base type, must be convertible to units of "seconds"
   * @return new value in the units of the "outputUnit
   * @throws ConversionException
   */
  public double convertTo(double value, SimpleUnit outputUnit) throws ConversionException {
    return uu.convertTo( value, outputUnit.getUnit());
  }

  public Date add( Date d) {
    Calendar cal = Calendar.getInstance();
    cal.setTime( d);
    cal.add( Calendar.SECOND, (int) getValueInSeconds());
    return (Date) cal.getTime();
  }

  /** TimeUnits with same value and unitString are equal */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TimeUnit)) return false;
    return o.hashCode() == this.hashCode();
  }

  /** Override hashcode to be consistent with equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37*result + unitString.hashCode();
      result = 37*result + (int) (1000.0 * value);
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0;

}