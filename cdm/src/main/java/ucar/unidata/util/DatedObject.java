/*
 * $Id: DatedObject.java,v 1.4 2007/05/11 13:56:46 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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








package ucar.unidata.util;


import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;



/**
 * A utility class that implements DatedThing
 */

public class DatedObject implements DatedThing {

    /** The date */
    private Date date;

    /** The object */
    private Object object;

    /**
     * Construct this object with just a date
     *
     * @param date the date
     */
    public DatedObject(Date date) {
        this(date, null);
    }


    /**
     * Construct this object with  a date and an object
     *
     * @param date the date
     * @param object The object
     */
    public DatedObject(Date date, Object object) {
        this.date   = date;
        this.object = object;
    }

    /**
     * Sort the given list of DatedThing-s
     *
     * @param datedThings list to sort
     * @param ascending sort order
     *
     * @return sorted list
     */
    public static List sort(List datedThings, final boolean ascending) {
        Comparator comp = new Comparator() {
            public int compare(Object o1, Object o2) {
                DatedThing a1     = (DatedThing) o1;
                DatedThing a2     = (DatedThing) o2;
                int        result = a1.getDate().compareTo(a2.getDate());
                if ( !ascending) {
                    result = -result;
                }
                return result;
            }
            public boolean equals(Object obj) {
                return obj == this;
            }
        };

        Object[] array = datedThings.toArray();
        Arrays.sort(array, comp);
        return Arrays.asList(array);
    }

    /**
     * equals method
     *
     * @param o object to check
     *
     * @return equals
     */
    public boolean equals(Object o) {
        if ( !(o instanceof DatedObject)) {
            return false;
        }
        DatedObject that = (DatedObject) o;
        if ( !this.date.equals(that.date)) {
            return false;
        }
        if (this.object == null) {
            return that.object == null;
        }
        if (that.object == null) {
            return this.object == null;
        }
        return this.object.equals(that.object);
    }





    /**
     * Set the Date property.
     *
     * @param value The new value for Date
     */
    public void setDate(Date value) {
        date = value;
    }

    /**
     * Get the Date property.
     *
     * @return The Date
     */
    public Date getDate() {
        return date;
    }

    /**
     * Set the Object property.
     *
     * @param value The new value for Object
     */
    public void setObject(Object value) {
        object = value;
    }

    /**
     * Get the Object property.
     *
     * @return The Object
     */
    public Object getObject() {
        return object;
    }


    /**
     * to string
     *
     * @return to string
     */
    public String toString() {
        if (object != null) {
            return date + " " + object;
        } else {
            return date.toString();
        }
    }

}

