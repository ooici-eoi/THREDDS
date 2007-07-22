/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

package ucar.ma2;

import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;


/**
   Represents a range of integers, used as an index set for arrays.
   It should be considered as a subset of the interval of integers [0, length-1] inclusive.
   Immutable.
   <p>
   Ranges are monotonically increasing.
   Elements must be nonnegative.
   EMPTY is the empty Range.
   <p> Note last is inclusive, so standard iteration is
   <pre>
       for (int i=range.first(); i<=range.last(); i+= range.stride()) {
         ...
       }
   or use:
      Range.Iterator iter = timeRange.getIterator();
      while (iter.hasNext()) {
        int index = iter.next();
        ...
   </pre>

 *
 * @author caron
 */

public final class Range {
  public static final Range EMPTY = new Range();

  private int n; // number of elements
  private int first; // first value in range
  private int stride = 1; // stride, must be >= 1
  //private String name; // optional name

  /**
   * Used for EMPTY
   */
  private Range() {
  }

  /**
     Create a range with unit stride.
     @param first	first value in range
     @param last	last value in range, inclusive
     @exception InvalidRangeException elements must be nonnegative, 0 <= first <= last
   */
  public Range(int first, int last) throws InvalidRangeException {
    this( first, last, 1);
  }


  /**
     Create a range with a specified stride.
     @param first	first value in range
     @param last	last value in range, inclusive
     @param stride	stride between consecutive elements (positive or negative)
     @exception InvalidRangeException elements must be nonnegative: 0 <= first <= last, stride >= 1
   */
  public Range(int first, int last, int stride) throws InvalidRangeException {
    if (first < 0)
      throw new InvalidRangeException("first must be >= 0");
    if (last < first)
      throw new InvalidRangeException("last must be >= first");
    if (stride < 1)
      throw new InvalidRangeException("stride must be > 0");

    this.first = first;
    this.stride = stride;
    this.n = Math.max( 1 + (last - first) / stride, 1);
  }

  /**
   * Copy Constructor
   * @param r copy from here
   */
  public Range(Range r) {
    first = r.first();
    n = r.length();
    stride = r.stride();
    //setName( r.getName());
  }


  /**
     Create a new Range by composing a Range that is reletive to this Range.
     @param r	range reletive to base
     @return combined Range, may be EMPTY
     @exception InvalidRangeException elements must be nonnegative, 0 <= first <= last
   */
  public Range compose(Range r) throws InvalidRangeException {
    if ((length() == 0) || (r.length() == 0))
      return EMPTY;

    int first = element( r.first());
    int stride = stride() * r.stride();
    int last = element(r.last());
    return new Range(first, last, stride);
  }

  /**
     Create a new Range shifting this range by a constant factor.
     @param origin subtract this from first, last
     @return shiften range
     @exception InvalidRangeException elements must be nonnegative, 0 <= first <= last
   */
  public Range shiftOrigin(int origin) throws InvalidRangeException {
    int first = first() - origin;
    int stride = stride();
    int last = last() - origin;
    return new Range(first, last, stride);
  }

  /** Get name
   * @return name, or null if none
   *
  public String getName() { return name; }

  /** Set name; used to track Dimensions
   * @param name name of Range
   *
  public void setName( String name) { this.name = name; } */

  /**
     Create a new Range by intersecting with a Range using same interval as this Range.
     NOTE: we dont yet support intersection when both Ranges have strides
     @param r	range to intersect
     @return intersected Range, may be EMPTY
     @exception InvalidRangeException elements must be nonnegative
   */
  public Range intersect(Range r) throws InvalidRangeException {
    if ((length() == 0) || (r.length() == 0))
      return EMPTY;

    int last = Math.min(this.last(), r.last());
    int stride = stride() * r.stride();

    int first;
    if (stride == 1) {
      first = Math.max(this.first(), r.first());

    } else if (stride() == 1) { // then r has a stride
      first = this.first();
      int rem = first % stride;
      if (rem > 0) // round up to multiple of stride
        first += stride - rem;
      first = Math.max(first, r.first());

    } else if (r.stride() == 1) { // then this has a stride
      first = r.first();
      int rem = first % stride;
      if (rem > 0) // round up to multiple of stride
        first += stride - rem;
      first = Math.max(first, this.first());

    } else {
      throw new UnsupportedOperationException("Intersection when both ranges have a stride");
    }

    if (first > last)
      return EMPTY;
    return new Range(first, last, stride);
  }

  /**
   * @return the number of elements in the range.
   */
  public int length() { return n; }
 
  /**
   * Get ith element
     @return the i-th element of a range.
     @param i	index of the element
     @exception InvalidRangeException i must be: 0 <= i < length
   */
  public int element(int i) throws InvalidRangeException {
    if (i < 0)
      throw new InvalidRangeException("i must be >= 0");
    if (i >= n)
      throw new InvalidRangeException("i must be < length");
    return first + i * stride;
  }

   /**
     Is the ith element contained in this Range?
     @param i	index in the original Range
     @return true if the ith element would be returned by the Range iterator
   */
  public boolean contains(int i) {
    if (i < first())
      return false;
    if (i > last())
      return false;
    if (stride == 1) return true;
    return (i-first) % stride == 0;
  }

  /**
   * Get ith element; skip checking, for speed.
   * @param i	index of the element
   * @return the i-th element of a range, no check
   */
  protected int elementNC(int i) {
    return first + i * stride;
  }

  /** @return first in range */
  public int first() {
    return first;
  }

  /** @return last in range, inclusive */
  public int last() {
    return first + (n - 1) * stride;
  }

  /** @return stride, must be >= 1*/
  public int stride() { return stride;  }

  /**
   * Iterate over Range index
   * Usage: <pre>
   * Iterator iter = range.getIterator();
   * while (iter.hasNext()) {
   *   int index = iter.next();
   *   doSomething(index);
   * }
   * </pre>
   * @return Iterator over element indices
   */
  public Iterator getIterator() { return new Iterator(); }
  public class Iterator {
    private int current = 0;
    public boolean hasNext() { return current < n; }
    public int next() {
      return elementNC(current++);
    }
  }

  /**
   * Find the smallest element k in the Range, such that <ul>
   * <li>k >= first
   * <li>k >= start
   * <li>k <= last
   * <li>k = first + i * stride for some integer i.
   * </ul>
   * @param start starting index
   * @return first in interval, else -1 if there is no such element.
   */
  public int getFirstInInterval(int start) {  // LOOK is this needed?
    if (start > last()) return -1;
    if (start <= first) return first;
    if (stride == 1) return start;
    int offset = start - first;
    int incr = offset % stride;
    int result = start + incr;
    return (result > last()) ? -1 : result;
  }

  public String toString() {
    return first+":"+last()+":"+stride;
  }

  /** Range elements with same first, last, stride are equal. */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Range)) return false;   // this catches nulls
    Range or =(Range) o;

    if ((n == 0) && (or.n == 0)) // empty ranges are equal
      return true;

    return (or.first == first) && (or.n == n) && (or.stride == stride);
  }

  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = first();
      result = 3700*result + last();
      result = 370*result + stride();
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0;


  //////////////////////////////////////////////////////////////////////////
  // deprecated
   /**
   * @return Minimum index, inclusive.
   * @deprecated use first()
   */
  public int min() {
    if (n > 0) {
      if (stride > 0)
        return first;
      else
        return first + (n - 1) * stride;
    }
    else {
      return first;
    }
  }

  /**
   * @return Maximum index, inclusive.
   * @deprecated use last()
   */
  public int max() {
    if (n > 0) {
      if (stride > 0)
        return first + (n - 1) * stride;
      else
        return first;
    }
    else {
      if (stride > 0)
        return first - 1;
      else
        return first + 1;
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // deprecated - use Section

  /** Convert shape array to List of Ranges. Assume 0 origin for all.
   * @deprecated use Section(int[] shape)
   */
  public static List factory( int[] shape) {
    ArrayList result = new ArrayList();
    for (int i=0; i<shape.length; i++ ) {
      try {
        result.add( new Range( 0, Math.max(shape[i]-1, -1)));
      } catch (InvalidRangeException e) {
        return null;
      }
    }
    return result;
  }

  /** Check rangeList has no nulls, set from shape array.
   * @deprecated use Section.setDefaults(int[] shape)
   */
  public static List setDefaults( List rangeList, int[] shape) {
    try {
      // entire rangeList is null
      if (rangeList == null) {
        rangeList = new ArrayList();
        for (int i = 0; i < shape.length; i++) {
          rangeList.add(new Range(0, shape[i]));
        }
        return rangeList;
      }

      // check that any individual range is null
      for (int i = 0; i < shape.length; i++) {
        Range r = (Range) rangeList.get(i);
        if (r == null) {
          rangeList.set(i, new Range(0, shape[i]-1));
        }
      }
      return rangeList;
    }
    catch (InvalidRangeException ex) {
      return null; // could happen if shape[i] is negetive
    }
  }

  /** Convert shape, origin array to List of Ranges.
   * @deprecated use Section(int[] origin, int[] shape)
   */
  public static List factory( int[] origin, int[] shape) throws InvalidRangeException {
    ArrayList result = new ArrayList();
    for (int i=0; i<shape.length; i++ ) {
      try {
        result.add(new Range(origin[i], origin[i] + shape[i] - 1));
      } catch (Exception e) {
        throw new InvalidRangeException( e.getMessage());
      }
    }
    return result;
  }

  /** Convert List of Ranges to shape array using the range.length.
   * @deprecated use Section.getShape()
   */
  public static int[] getShape( List ranges) {
    if (ranges == null) return null;
    int[] result = new int[ranges.size()];
    for (int i=0; i<ranges.size(); i++ ) {
      result[i] = ((Range)ranges.get(i)).length();
    }
    return result;
  }

  /**
   * @deprecated use Section.toString()
   */
  public static String toString(List ranges) {
    if (ranges == null) return "";
    StringBuffer sbuff = new StringBuffer();
    for (int i=0; i<ranges.size(); i++ ) {
      if (i>0) sbuff.append(",");
      sbuff.append(((Range)ranges.get(i)).length());
    }
    return sbuff.toString();
  }

  /**
   /** Compute total number of elements represented by the section.
   * @param section List of Range objects
   * @return total number of elements
   * @deprecated use Section.computeSize()
   */
  static public long computeSize(List section) {
    int[] shape = getShape( section);
    return Index.computeSize( shape);
  }

  /**
   * Append a new Range(0,size-1) to the list
   * @param ranges list of Range
   * @param size add this Range
   * @return same list
   * @throws InvalidRangeException if size < 1
   * @deprecated use Section.appendRange(int size)
   */
  public static List appendShape( List ranges, int size) throws InvalidRangeException {
    ranges.add( new Range(0, size-1));
    return ranges;
  }

  /** Convert List of Ranges to origin array using the range.first.
   *  @deprecated use Section.getOrigin()
   */
  public static int[] getOrigin( List ranges) {
    if (ranges == null) return null;
    int[] result = new int[ranges.size()];
    for (int i=0; i<ranges.size(); i++ ) {
      result[i] = ((Range)ranges.get(i)).first();
    }
    return result;
  }

  /** Convert List of Ranges to array of Ranges.  *
   *  @deprecated use Section.getRanges()
   */
  public static Range[] toArray( List ranges) {
    if (ranges == null) return null;
    return (Range[]) ranges.toArray( new Range[ ranges.size()] );
  }

  /** Convert array of Ranges to List of Ranges.
   *  @deprecated use Section.getRanges()
   */
  public static List toList( Range[] ranges) {
    if (ranges == null) return null;
    return java.util.Arrays.asList( ranges);
  }

  /** Convert List of Ranges to String Spec.
   *  Inverse of parseSpec
   *  @deprecated use Section.toString()
   */
  public static String makeSectionSpec( List ranges) {
    StringBuffer sbuff = new StringBuffer();
    for (int i = 0; i < ranges.size(); i++) {
      Range r = (Range) ranges.get(i);
      if (i>0) sbuff.append(",");
      sbuff.append(r.toString());
    }
    return sbuff.toString();
  }


  /**
   * Parse an index section String specification, return equivilent list of ucar.ma2.Range objects.
   * The sectionSpec string uses fortran90 array section syntax, namely:
   * <pre>
   *   sectionSpec := dims
   *   dims := dim | dim, dims
   *   dim := ':' | slice | start ':' end | start ':' end ':' stride
   *   slice := INTEGER
   *   start := INTEGER
   *   stride := INTEGER
   *   end := INTEGER
   *
   * where nonterminals are in lower case, terminals are in upper case, literals are in single quotes.
   *
   * Meaning of index selector :
   *  ':' = all
   *  slice = hold index to that value
   *  start:end = all indices from start to end inclusive
   *  start:end:stride = all indices from start to end inclusive with given stride
   *
   * </pre>
   *
   * @param sectionSpec the token to parse, eg "(1:20,:,3,10:20:2)", parenthesis optional
   * @return return List of ucar.ma2.Range objects corresponding to the index selection. A null
   *   Range means "all" (i.e.":") indices in that dimension.
   *
   * @throws IllegalArgumentException when sectionSpec is misformed
   * @deprecated use new Section(String sectionSpec)
   */
  public static List parseSpec(String sectionSpec) throws InvalidRangeException {

    ArrayList result = new ArrayList();
    Range section;

    StringTokenizer stoke = new StringTokenizer(sectionSpec,"(),");
    while (stoke.hasMoreTokens()) {
      String s = stoke.nextToken().trim();
      if (s.equals(":"))
        section = null; // all

      else if (s.indexOf(':') < 0) { // just a number : slice
        try {
          int index = Integer.parseInt(s);
          section = new Range( index, index);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException(" illegal selector: "+s+" part of <"+sectionSpec+">");
        }

      } else {  // gotta be "start : end" or "start : end : stride"
        StringTokenizer stoke2 = new StringTokenizer(s,":");
        String s1 = stoke2.nextToken();
        String s2 = stoke2.nextToken();
        String s3 = stoke2.hasMoreTokens() ? stoke2.nextToken() : null;
        try {
          int index1 = Integer.parseInt(s1);
          int index2 = Integer.parseInt(s2);
          int stride = (s3 != null) ? Integer.parseInt(s3) : 1;
          section = new Range( index1, index2, stride);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException(" illegal selector: "+s+" part of <"+sectionSpec+">");
        }
      }

      result.add(section);
    }

    return result;
  }

  /**
   * Check ranges are valid
   * @param section
   * @param shape
   * @return
   * @deprecated use Section.checkInRange(int shape[])
   */
  public static String checkInRange( List section, int shape[]) {
    if (section.size() != shape.length)
      return "Number of ranges in section must be ="+shape.length;
    for (int i=0; i<section.size(); i++) {
      Range r = (Range) section.get(i);
      if (r == null) continue;
      if (r.last() >= shape[i])
        return "Illegal range for dimension "+i+": requested "+r.last()+" >= max "+shape[i];
    }

    return null;
  }
}