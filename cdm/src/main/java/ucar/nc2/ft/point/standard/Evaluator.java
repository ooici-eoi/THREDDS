/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ft.point.standard;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.nc2.Structure;
import ucar.nc2.Attribute;

import java.util.Formatter;

/**
 * Helper routines for Nested Tables
 * @author caron
 * @since Apr 23, 2008
 */
public class Evaluator {

  /**
   * Turn the key into a String and return the corresponding featureType, if any.
   * @param ds look in this datset
   * @param key if starts with ":", replace with value of global attribute
   * @param errlog error messages here
   * @return
   */
  static public FeatureType getFeatureType(NetcdfDataset ds, String key, Formatter errlog) {
    FeatureType ft = null;
    String fts = getLiteral(ds, key, errlog);
    if (fts != null) {
      ft = FeatureType.valueOf(fts.toUpperCase());
      if ((ft == null) && (errlog != null))
        errlog.format(" Cant find Feature type %s from %s\n", fts, key);
    }
    return ft;
  }

  /**
   * Translate key to value
   * @param ds look in this datset
   * @param key if starts with ":", look for global attribute
   * @param errlog error messages here
   * @return return global attribute value or the key itself
   */
  static public String getLiteral(NetcdfDataset ds, String key, Formatter errlog) {
    if (key.startsWith(":")) {
      String val = ds.findAttValueIgnoreCase(null, key.substring(1), null);
      if ((val == null) && (errlog != null))
        errlog.format(" Cant find global attribute %s\n", key);
      return val;
    }

    return key;
  }

  static public String getVariableName(NetcdfDataset ds, String key, Formatter errlog) {
    Variable v = null;
    String vs = getLiteral(ds, key, errlog);
    if (vs != null) {
      v = ds.findVariable(vs);
      if ((v == null) && (errlog != null))
        errlog.format(" Cant find Variable %s from %s\n", vs, key);
    }
    return v == null ? null : v.getShortName();
  }

  static public String getVariableWithAttribute(NetcdfDataset ds, String attName, String attValue) {
    for (Variable v : ds.getVariables()) {
      String stdName = ds.findAttValueIgnoreCase(v, attName, null);
      if ((stdName != null) && stdName.equals(attValue))
        return v.getName();
    }
    return null;
  }

  static public String getVariableWithAttribute(Structure struct, String attName, String attValue) {
    for (Variable v : struct.getVariables()) {
      Attribute att = v.findAttributeIgnoreCase(attName);
      if ((att != null) && att.getStringValue().equals(attValue))
        return v.getShortName();
    }
    return null;
  }

  static public Dimension getDimension(NetcdfDataset ds, String key, Formatter errlog) {
    Dimension d = null;
    String s = getLiteral(ds, key, errlog);
    if (s != null) {
      d = ds.findDimension(s); // LOOK use group
      if ((d == null) && (errlog != null))
        errlog.format(" Cant find Variable %s from %s\n", s, key);
    }
    return d;
  }


  /////////////////////////////////
  private String constant;
  private Variable v;

  Evaluator(Variable v) {
    this.v = v;
  }

  Evaluator(String constant) {
    this.constant = constant;
  }

  public String getValue() {
    if (constant != null)
      return constant;
    return null; // ??
  }

}
