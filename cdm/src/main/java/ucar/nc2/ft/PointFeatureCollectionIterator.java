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

package ucar.nc2.ft;

/**
 * An iterator over PointFeatureCollections.
 *
 * @author caron
 */
public interface PointFeatureCollectionIterator {

  /**
   * true if another PointFeatureCollection is available
   * @return true if another PointFeatureCollection is available
   * @throws java.io.IOException on i/o error
   */
  public boolean hasNext() throws java.io.IOException;

  /**
   * Returns the next PointFeatureCollection
   * You must call hasNext() before calling next(), even if you know it will return true.
   * @return the next PointFeatureCollection 
   * @throws java.io.IOException on i/o error
   */
  public PointFeatureCollection next() throws java.io.IOException;

  /**
   * Hint to use this much memory in buffering the iteration.
   * No guarentee that it will be used by the implementation.
   * @param bytes amount of memory in bytes
   */
  public void setBufferSize( int bytes);

  /**
   * A filter on PointFeatureCollection.
   */
  public interface Filter {
    public boolean filter(PointFeatureCollection pointFeatureCollection);
  }

}
