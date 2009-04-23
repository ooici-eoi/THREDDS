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

package ucar.nc2.dataset;

import ucar.nc2.TestAll;
import ucar.nc2.*;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.geoloc.vertical.*;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.projection.*;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * test various transforms that we have test data for.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class TestTransforms extends TestCase {

  public TestTransforms(String name) {
    super(name);
  }

  public void testHybridSigmaPressure() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + "grid/transforms/HybridSigmaPressure.nc";
    test(filename, "lev", "T", "time", VerticalCT.Type.HybridSigmaPressure, HybridSigmaPressure.class,
            SimpleUnit.pressureUnit );
  }

  public void testHybridSigmaPressure2() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + "grid/netcdf/cf/climo.cam2.h0.0000-09.nc";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VerticalTransform vt = test(ncd, "lev", "T", "time", VerticalCT.Type.HybridSigmaPressure, HybridSigmaPressure.class,
            SimpleUnit.pressureUnit );

    Dimension timeDim = ncd.findDimension("time");
    for (int i = 0; i < timeDim.getLength(); i++) {
      ucar.ma2.ArrayDouble.D3 coordVals = vt.getCoordinateArray(i);
      int[] shape = coordVals.getShape();
      assert shape[0] == ncd.findDimension("lev").getLength();
      assert shape[1] == ncd.findDimension("lat").getLength();
      assert shape[2] == ncd.findDimension("lon").getLength();
    }

    ncd.close();
  }

  public void testOceanS() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + "grid/transforms/OceanS.nc";
    test(filename, "s_rho", "salt", "ocean_time", VerticalCT.Type.OceanS, OceanS.class, SimpleUnit.meterUnit);
  }

  public void testOceanS2() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + "grid/transforms/OceanS2.nc";
    test(filename, "s_rho", "temp", "ocean_time", VerticalCT.Type.OceanS, OceanS.class, SimpleUnit.meterUnit);
  }

  public void testOceanSigma() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + "grid/transforms/OceanSigma.nc";
    test(filename, "zpos", "salt", "time", VerticalCT.Type.OceanSigma, OceanSigma.class, SimpleUnit.meterUnit);
  }

  public void testOceanS3() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + "grid/transforms/ocean_his.nc";
    test(filename, "s_rho", "u", "ocean_time", VerticalCT.Type.OceanS, OceanS.class, SimpleUnit.meterUnit);
  }

  public void testOceanG1() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + "grid/transforms/ocean_his_g1.nc";
    test(filename, "s_rho", "u", "ocean_time", VerticalCT.Type.OceanSG1, OceanSG1.class, SimpleUnit.meterUnit);
  }

  public void testOceanG2() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + "grid/transforms/ocean_his_g2.nc";
    test(filename, "s_rho", "u", "ocean_time", VerticalCT.Type.OceanSG2, OceanSG2.class, SimpleUnit.meterUnit);
  }

  public void testSigma() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + "grid/transforms/Sigma_LC.nc";
    test(filename, "level", "Temperature", null, VerticalCT.Type.Sigma, AtmosSigma.class, SimpleUnit.pressureUnit );
  }

  public void testExisting3D() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + "grid/transforms/VExisting3D_NUWG.nc";
    test(filename, "VerticalTransform", "rhu_hybr", "record", VerticalCT.Type.Existing3DField, VTfromExistingData.class,
            null);
  }

  private VerticalTransform test(String filename, String levName, String varName, String timeName,
          VerticalCT.Type vtype, Class vclass, SimpleUnit unit)
          throws IOException, InvalidRangeException {

    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    test(ncd, levName, varName, timeName, vtype,  vclass, unit);
    ncd.close();
    return null;
  }

  private VerticalTransform test(NetcdfDataset ncd, String levName, String varName, String timeName,
          VerticalCT.Type vtype, Class vclass, SimpleUnit vunit)
          throws IOException, InvalidRangeException {

    VariableDS lev = (VariableDS) ncd.findVariable(levName);
    assert lev != null;
    System.out.println(" dump of ctv = \n" + lev);

    VariableDS v = (VariableDS) ncd.findVariable(varName);
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List vList = new ArrayList();
    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    for (int i = 0; i < tList.size(); i++) {
      CoordinateTransform ct = (CoordinateTransform) tList.get(i);
      if (ct.getTransformType() == TransformType.Vertical)
        vList.add(ct);
    }
    assert vList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) vList.get(0);
    assert ct.getTransformType() == TransformType.Vertical;
    assert ct instanceof VerticalCT;

    VerticalCT vct = (VerticalCT) ct;
    assert vct.getVerticalTransformType() == vtype : vct.getVerticalTransformType();

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    VerticalTransform vt = null;
    if (timeName == null) {
      vt = vct.makeVerticalTransform(ncd, null);
      assert !vt.isTimeDependent();
      ucar.ma2.Array coordVals = vt.getCoordinateArray(0);
      assert (null != coordVals);

    } else {
      Dimension timeDim = ncd.findDimension(timeName);
      assert null != timeDim;
      vt = vct.makeVerticalTransform(ncd, timeDim);
      assert vt.isTimeDependent();
      for (int i = 0; i < timeDim.getLength(); i++) {
        ucar.ma2.ArrayDouble.D3 coordVals = vt.getCoordinateArray(i);
        assert (null != coordVals);
      }
    }
    assert vt != null;
    assert vclass.isInstance(vt);

    // should be compatible with vunit
    if (vunit != null) {
      String vertCoordUnit = vt.getUnitString();
      assert vunit.isCompatible(vertCoordUnit) : vertCoordUnit + " not udunits compatible with " + vunit.getUnitString();
    }

    return vt;
  }


 /////////////////////////////////////////////////////////////////////////

  public void testLC() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + "grid/transforms/Sigma_LC.nc";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VariableDS lev = (VariableDS) ncd.findVariable("Lambert_Conformal");
    assert lev != null;
    System.out.println(" dump of ctv = \n" + lev);

    VariableDS v = (VariableDS) ncd.findVariable("Temperature");
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List pList = new ArrayList();
    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    for (int i = 0; i < tList.size(); i++) {
      CoordinateTransform ct = (CoordinateTransform) tList.get(i);
      if (ct.getTransformType() == TransformType.Projection)
        pList.add(ct);
    }
    assert pList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) pList.get(0);
    assert ct.getTransformType() == TransformType.Projection;
    assert ct instanceof ProjectionCT;

    ProjectionCT vct = (ProjectionCT) ct;
    Projection proj = vct.getProjection();
    assert proj != null;
    assert proj instanceof LambertConformal;

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    ncd.close();
  }

  public void testLA() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + "grid/transforms/LambertAzimuth.nc";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VariableDS lev = (VariableDS) ncd.findVariable("grid_mapping0");
    assert lev != null;
    System.out.println(" dump of ctv = \n" + lev);

    VariableDS v = (VariableDS) ncd.findVariable("VIL");
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List pList = new ArrayList();
    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    for (int i = 0; i < tList.size(); i++) {
      CoordinateTransform ct = (CoordinateTransform) tList.get(i);
      if (ct.getTransformType() == TransformType.Projection)
        pList.add(ct);
    }
    assert pList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) pList.get(0);
    assert ct.getTransformType() == TransformType.Projection;
    assert ct instanceof ProjectionCT;

    ProjectionCT vct = (ProjectionCT) ct;
    Projection proj = vct.getProjection();
    assert proj != null;
    assert proj instanceof LambertAzimuthalEqualArea;

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    ncd.close();
  }

  public void testPS() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + "grid/transforms/PolarStereographic.nc";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VariableDS lev = (VariableDS) ncd.findVariable("Polar_Stereographic");
    assert lev != null;
    System.out.println(" dump of ctv = \n" + lev);

    VariableDS v = (VariableDS) ncd.findVariable("D2_O3");
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List pList = new ArrayList();
    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    for (int i = 0; i < tList.size(); i++) {
      CoordinateTransform ct = (CoordinateTransform) tList.get(i);
      if (ct.getTransformType() == TransformType.Projection)
        pList.add(ct);
    }
    assert pList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) pList.get(0);
    assert ct.getTransformType() == TransformType.Projection;
    assert ct instanceof ProjectionCT;

    ProjectionCT vct = (ProjectionCT) ct;
    Projection proj = vct.getProjection();
    assert proj != null;
    assert proj instanceof Stereographic;

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    ncd.close();
  }

  public void testPS2() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + "grid/transforms/Polar_Stereographic2.nc";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);

    VariableDS v = (VariableDS) ncd.findVariable("dpd-Surface0");
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List pList = new ArrayList();
    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    for (int i = 0; i < tList.size(); i++) {
      CoordinateTransform ct = (CoordinateTransform) tList.get(i);
      if (ct.getTransformType() == TransformType.Projection)
        pList.add(ct);
    }
    assert pList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) pList.get(0);
    assert ct.getTransformType() == TransformType.Projection;
    assert ct instanceof ProjectionCT;

    ProjectionCT vct = (ProjectionCT) ct;
    Projection proj = vct.getProjection();
    assert proj != null;
    assert proj instanceof Stereographic;

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    ncd.close();
  }

  public void testPS3() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + "grid/netcdf/cf/Base_month.nc";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);

    VariableDS v = (VariableDS) ncd.findVariable("D2_SO4");
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List pList = new ArrayList();
    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    for (int i = 0; i < tList.size(); i++) {
      CoordinateTransform ct = (CoordinateTransform) tList.get(i);
      if (ct.getTransformType() == TransformType.Projection)
        pList.add(ct);
    }
    assert pList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) pList.get(0);
    assert ct.getTransformType() == TransformType.Projection;
    assert ct instanceof ProjectionCT;

    ProjectionCT vct = (ProjectionCT) ct;
    Projection proj = vct.getProjection();
    assert proj != null;
    assert proj instanceof Stereographic;

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    ncd.close();
  }

  public void testMercator() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + "grid/transforms/Mercator.grib1";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VariableDS lev = (VariableDS) ncd.findVariable("Mercator_Projection_Grid");
    assert lev != null;
    System.out.println(" dump of ctv = \n" + lev);

    VariableDS v = (VariableDS) ncd.findVariable("Temperature");
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List pList = new ArrayList();
    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    for (int i = 0; i < tList.size(); i++) {
      CoordinateTransform ct = (CoordinateTransform) tList.get(i);
      if (ct.getTransformType() == TransformType.Projection)
        pList.add(ct);
    }
    assert pList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) pList.get(0);
    assert ct.getTransformType() == TransformType.Projection;
    assert ct instanceof ProjectionCT;

    ProjectionCT vct = (ProjectionCT) ct;
    Projection proj = vct.getProjection();
    assert proj != null;
    assert proj instanceof Mercator : proj.getClass().getName();

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    ncd.close();
  }

  public void testVerticalPerspective() throws IOException, InvalidRangeException {
    String filename = TestAll.testdataDir + "grid/transforms/Eumetsat.VerticalPerspective.grb";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    VariableDS lev = (VariableDS) ncd.findVariable("Space_View_Perspective_or_Orthographic");
    assert lev != null;
    System.out.println(" dump of ctv = \n" + lev);

    VariableDS v = (VariableDS) ncd.findVariable("Pixel_scene_type");
    assert v != null;

    List cList = v.getCoordinateSystems();
    assert cList != null;
    assert cList.size() == 1;
    CoordinateSystem csys = (CoordinateSystem) cList.get(0);

    List pList = new ArrayList();
    List tList = csys.getCoordinateTransforms();
    assert tList != null;
    for (int i = 0; i < tList.size(); i++) {
      CoordinateTransform ct = (CoordinateTransform) tList.get(i);
      if (ct.getTransformType() == TransformType.Projection)
        pList.add(ct);
    }
    assert pList.size() == 1;
    CoordinateTransform ct = (CoordinateTransform) pList.get(0);
    assert ct.getTransformType() == TransformType.Projection;
    assert ct instanceof ProjectionCT;

    ProjectionCT vct = (ProjectionCT) ct;
    Projection proj = vct.getProjection();
    assert proj != null;
    assert proj instanceof VerticalPerspectiveView : proj.getClass().getName();

    VariableDS ctv = CoordTransBuilder.makeDummyTransformVariable(ncd, ct);
    System.out.println(" dump of equivilent ctv = \n" + ctv);

    ncd.close();
  }

}
