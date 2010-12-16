/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ucar.nc2.ft.mesh;

import ucar.nc2.ft.FeatureDatasetFactory;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dt.mesh.MeshDataset;

import java.io.IOException;
import java.util.Formatter;
/**
 *
 * @author Kyle
 */
public class MeshDatasetStandardFactory implements FeatureDatasetFactory {
  
  public Object isMine(FeatureType wantFeatureType, NetcdfDataset ncd, Formatter errlog) throws IOException {
    // If they ask for a grid, and there seems to be some grids, go for it
    if (wantFeatureType == FeatureType.MESH ) {
      ucar.nc2.dt.mesh.MeshDataset mds = new ucar.nc2.dt.mesh.MeshDataset( ncd);
      return mds;
    }
    return null;
  }

  public FeatureDataset open(FeatureType ftype, NetcdfDataset ncd, Object analysis, CancelTask task, Formatter errlog) throws IOException {
    // already been opened by isMine
    return (MeshDataset) analysis;
  }

  public FeatureType[] getFeatureType() {
    return new FeatureType[] {FeatureType.MESH};
  }
}
