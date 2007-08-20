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
package ucar.nc2.dataset;

/**
 * Public interface to an "enhanced Variable", implemented by the ucar.nc2.dataset package.
 * @author john caron
 */

public interface VariableEnhanced extends ucar.nc2.VariableIF, Enhancements, EnhanceScaleMissing {

  public ucar.nc2.Variable getOriginalVariable();
  public void setOriginalVariable(ucar.nc2.Variable orgVar);

  public ProxyReader2 getProxyReader2();
  public void setProxyReader2( ProxyReader2 proxyReader2);

  /**
   * Process scale/offset/missing value
   */
  public void enhance();
}
