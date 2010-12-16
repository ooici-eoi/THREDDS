/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ucar.nc2.dt;

import java.util.*;

/**
 *
 * @author Kyle
 */
public interface MeshDataset extends ucar.nc2.dt.TypedDataset {

  /** get the list of MeshDatatype objects contained in this dataset.
   * @return  list of MeshDatatype
   */
  public List<MeshDatatype> getGrids();

  /** find the named MeshDatatype.
   * @param name look for this name
   * @return  the named MeshDatatype, or null if not found
   */
  public MeshDatatype findMeshDatatype( String name);

  /**
   * Return GridDatatype objects grouped by GridCoordSystem. All GridDatatype in a Meshset
   *   have the same GridCoordSystem.
   * @return List of type MeshDataset.Meshset
   */
  public List<Meshset> getMeshsets();

  /**
   * A set of GridDatatype objects with the same Coordinate System.
   */
  public interface Meshset {

    /** Get list of MeshDatatype objects with same Coordinate System
     * @return list of MeshDatatype
     */
    public List<MeshDatatype> getGrids();

    /** all the MeshDatatype in this Meshset use this MeshCoordSystem
     * @return  the common MeshCoordSystem
     */
    public ucar.nc2.dt.MeshCoordSystem getGeoCoordSystem();
  }
}
