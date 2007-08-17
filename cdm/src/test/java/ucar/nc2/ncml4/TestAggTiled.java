package ucar.nc2.ncml4;

import junit.framework.*;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ncml.TestNcML;

import java.io.IOException;

/**
 * Test TestNcml - AggExisting  in the JUnit framework.
 */

public class TestAggTiled extends TestCase {
  int nlon = 24;
  int nlat = 6;

  public TestAggTiled(String name) {
    super(name);
  }

  public void testNcmlDirect() throws IOException, InvalidRangeException {
    String filename = "file:./" + TestNcML.topDir + "tiled/testAggTiled.ncml";

    NetcdfFile ncfile = new NcMLReader().readNcML(filename, null);
    System.out.println(" TestNcmlAggExisting.open " + ncfile);

    testDimensions(ncfile);
    testCoordVar(ncfile, "lat", nlat, DataType.DOUBLE);
    testCoordVar(ncfile, "lon", nlon, DataType.FLOAT);
    testReadData(ncfile);
    //testReadSlice(ncfile);
    ncfile.close();
  }

  public void testDimensions(NetcdfFile ncfile) {
    Dimension latDim = ncfile.findDimension("lat");
    assert null != latDim;
    assert latDim.getName().equals("lat");
    assert latDim.getLength() == nlat;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("lon");
    assert null != lonDim;
    assert lonDim.getName().equals("lon");
    assert lonDim.getLength() == nlon;
    assert !lonDim.isUnlimited();
  }

  public void testCoordVar(NetcdfFile ncfile, String name, int n, DataType type) throws IOException {

    Variable lat = ncfile.findVariable(name);
    assert null != lat;
    assert lat.getName().equals(name);
    assert lat.getRank() == 1;
    assert lat.getSize() == n;
    assert lat.getShape()[0] == n;
    assert lat.getDataType() == type;

    assert !lat.isUnlimited();
    assert lat.getDimension(0).equals(ncfile.findDimension(name));

    Array data = lat.read();
    assert data.getRank() == 1;
    assert data.getSize() == n;
    assert data.getShape()[0] == n;
    assert data.getElementType() == type.getPrimitiveClassType();

    int count = 0;
    IndexIterator dataI = data.getIndexIterator();
    while (dataI.hasNext())
      assert TestUtils.close(dataI.getDoubleNext(), (double) count++);

  }

  public void testReadData(NetcdfFile ncfile) {
    Variable v = ncfile.findVariable("temperature");
    assert null != v;
    assert v.getName().equals("temperature");
    assert v.getRank() == 2;
    assert v.getSize() == nlon * nlat : v.getSize();
    assert v.getShape()[0] == nlat;
    assert v.getShape()[1] == nlon;
    assert v.getDataType() == DataType.DOUBLE;

    assert !v.isCoordinateVariable();

    assert v.getDimension(0) == ncfile.findDimension("lat");
    assert v.getDimension(1) == ncfile.findDimension("lon");

    try {
      Array data = v.read();
      assert data.getRank() == 2;
      assert data.getSize() == nlon * nlat;
      assert data.getShape()[0] == nlat;
      assert data.getShape()[1] == nlon;
      assert data.getElementType() == double.class;

      int[] shape = data.getShape();
      Index tIndex = data.getIndex();
      for (int row = 0; row < shape[0]; row++)
        for (int col = 0; col < shape[1]; col++) {
          double val = data.getDouble( tIndex.set(row, col));

          double truth = (col < 12 ) ? row * 12 + col : 72 + row * 12 + (col-12);

          // System.out.println(" "+val);
          assert TestUtils.close(val, truth) : val + "!=" + truth+"("+row+","+col+")";
        }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }
  }

  public void testReadSlice(NetcdfFile ncfile, int[] origin, int[] shape) throws IOException, InvalidRangeException {

    Variable v = ncfile.findVariable("T");

    Array data = v.read(origin, shape);
    assert data.getRank() == 3;
    assert data.getSize() == shape[0] * shape[1] * shape[2];
    assert data.getShape()[0] == shape[0] : data.getShape()[0] + " " + shape[0];
    assert data.getShape()[1] == shape[1];
    assert data.getShape()[2] == shape[2];
    assert data.getElementType() == double.class;

    Index tIndex = data.getIndex();
    for (int i = 0; i < shape[0]; i++)
      for (int j = 0; j < shape[1]; j++)
        for (int k = 0; k < shape[2]; k++) {
          double val = data.getDouble(tIndex.set(i, j, k));
          //System.out.println(" "+val);
          assert TestUtils.close(val, 100 * (i + origin[0]) + 10 * j + k) : val;
        }

  }

  public void testReadSlice(NetcdfFile ncfile) throws IOException, InvalidRangeException {
    testReadSlice(ncfile, new int[]{0, 0, 0}, new int[]{59, 3, 4});
    testReadSlice(ncfile, new int[]{0, 0, 0}, new int[]{2, 3, 2});
    testReadSlice(ncfile, new int[]{25, 0, 0}, new int[]{10, 3, 4});
    testReadSlice(ncfile, new int[]{44, 0, 0}, new int[]{10, 2, 3});
  }
}
