// $Id: SelectGeoRegion.java 48 2006-07-12 16:15:40Z caron $
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

package thredds.catalog.query;

/**
 * Implementation of a DQC select geo region.
 *
 * @author john caron
 */

public class SelectGeoRegion extends Selector {
  private Location lowerLeft, upperRight;

   /**
    * Construct from fields in XML catalog.
    * @see Selector
    */
  public SelectGeoRegion( Location lowerLeft, Location upperRight) {
    super();

    this.lowerLeft = lowerLeft;
    this.upperRight = upperRight;
  }

  public Location getLowerLeft() { return lowerLeft; }
  public Location getUpperRight() { return upperRight; }

   public boolean equals(Object o) {
     if (this == o) return true;
     if (!(o instanceof SelectGeoRegion)) return false;
     return o.hashCode() == this.hashCode();
  }

  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      if (getTitle() != null)
        result = 37*result + getTitle().hashCode();
      if (getId() != null)
        result = 37*result + getId().hashCode();
      if (getTemplate() != null)
        result = 37*result + getTemplate().hashCode();
      if (isRequired()) result++;
      if (isMultiple()) result++;

      if (getLowerLeft() != null)
        result = 37*result + getLowerLeft().hashCode();
      if (getUpperRight() != null)
        result = 37*result + getUpperRight().hashCode();

      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0; // Bloch, item 8

}