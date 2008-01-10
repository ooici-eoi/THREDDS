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

package ucar.nc2.iosp.hdf5;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.FileFilter;
import java.io.File;

import ucar.nc2.*;
import ucar.nc2.iosp.hdf4.H4header;
import ucar.unidata.util.StringUtil;

/**
 * Class Description.
 *
 * @author caron
 */
public class TestH5ReadAndCount extends TestCase {

  static public String testDir = "R:/testdata/hdf5/";

  public TestH5ReadAndCount(String name) {
    super(name);
  }

  public void testReadAndCount() throws IOException {
    System.out.println("  dims  vars gatts  atts strFlds groups");
    read(testDir + "support/astrarr.h5", 0, 1, 0, 2, 0, 0);
    read(testDir + "support/attstr.h5", 0, 0, 1, 0, 0, 1);
    read(testDir + "support/bitop.h5", 0, 4, 0, 6, 0, 1);
    read(testDir + "support/bool.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "support/cenum.h5", 0, 1, 0, 1, 1, 0);
    read(testDir + "support/cstr.h5", 0, 1, 0, 1, 2, 0);
    read(testDir + "support/cuslab.h5", 0, 1, 0, 1, 3, 0);
    read(testDir + "support/dstr.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "support/dstrarr.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "support/DSwith_array_member.h5", 0, 1, 0, 1, 2, 0);
    read(testDir + "support/enum.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "support/SDS_array_type.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "support/short.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "support/time.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "support/uvlstr.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "support/vlslab.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "support/vlstra.h5", 0, 0, 1, 0, 0, 0);
    read(testDir + "support/zip.h5", 0, 1, 0, 1, 0, 1);
    read(testDir + "samples/bitfield.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "samples/enum.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "samples/enumcmpnd.h5", 0, 1, 0, 1, 1, 0);
    read(testDir + "samples/f32be.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "samples/f32le.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "samples/f64be.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "samples/f64le.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "samples/i16be.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "samples/i16le.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "samples/i32be.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "samples/i32le.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "samples/i64be.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "samples/i64le.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "samples/i8be.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "samples/i8le.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "samples/il32be.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "samples/il32le.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "samples/opaque.h5", 0, 1, 0, 2, 0, 0);
    read(testDir + "samples/strings.h5", 0, 1, 0, 1, 0, 0);
    read(testDir + "samples/u16be.h5", 0, 1, 0, 2, 0, 0);
    read(testDir + "samples/u16le.h5", 0, 1, 0, 2, 0, 0);
    read(testDir + "samples/u32be.h5", 0, 1, 0, 2, 0, 0);
    read(testDir + "samples/u32le.h5", 0, 1, 0, 2, 0, 0);
    read(testDir + "samples/u64be.h5", 0, 1, 0, 2, 0, 0);
    read(testDir + "samples/u64le.h5", 0, 1, 0, 2, 0, 0);
    read(testDir + "samples/u8be.h5", 0, 1, 0, 2, 0, 0);
    read(testDir + "samples/u8le.h5", 0, 1, 0, 2, 0, 0);
    read(testDir + "samples/ul32be.h5", 0, 1, 0, 2, 0, 0);
    read(testDir + "samples/ul32le.h5", 0, 1, 0, 2, 0, 0);
    read(testDir + "complex/compound_complex.h5", 0, 1, 0, 1, 7, 0);
    read(testDir + "complex/compound_native.h5", 0, 1, 0, 1, 12, 0);

    read(testDir + "msg/test.h5", 0, 57, 157, 204, 0, 60);

    read(testDir + "wrf/wrf_bdy_par.h5", 0, 50, 44, 338, 3, 3);
    read(testDir + "wrf/wrf_input_par.h5", 0, 110, 43, 758, 3, 3);
    read(testDir + "wrf/wrf_out_par.h5", 0, 73, 43, 499, 3, 3);
    read(testDir + "wrf/wrf_bdy_seq.h5", 0, 50, 44, 338, 3, 2);
    read(testDir + "wrf/wrf_input_seq.h5", 0, 110, 43, 758, 3, 2);
    read(testDir + "wrf/wrf_out_seq.h5", 0, 73, 43, 499, 3, 2);

    read(testDir + "npoess/ExampleFiles/AVAFO_NPP_d2003125_t10109_e101038_b9_c2005829155458_devl_Tst.h5", 0, 7, 12, 51, 0, 4);
    read(testDir + "npoess/ExampleFiles/GDNBF-VNCCO_NPP_d2003125_t101038_e10116_b9_c2005829162517_dev.h5", 0, 24, 16, 104, 0, 6);
    read(testDir + "npoess/ExampleFiles/GIMFT-VIVIO_NPP_d2003125_t101038_e10116_b9_c2005829173243_dev.h5", 0, 20, 16, 98, 0, 6);
    read(testDir + "npoess/ExampleFiles/GMOFT-VOCCO_NPP_d2003125_t101038_e10116_b9_c2005829163632_dev.h5", 0, 41, 16, 134, 0, 6);
    read(testDir + "npoess/ExampleFiles/GMOFT-VSSTO_NPP_d2003125_t101038_e10116_b9_c2005829165313_dev.h5", 0, 25, 16, 109, 0, 6);
    read(testDir + "npoess/ExampleFiles/RVIRS_NPP_d2003125_t101038_e10116_b9_c200582917556_devl_Tst.h5", 0, 3, 8, 26, 0, 4);
    read(testDir + "npoess/ExampleFiles/SVI01-GIMFG_NPP_d2003125_t101038_e10116_b9_c2005829153351_dev.h5", 0, 29, 15, 116, 0, 6);
    read(testDir + "npoess/ExampleFiles/SVM04-GMOFG_NPP_d2003125_t101038_e10116_b9_c2005829171444_dev.h5", 0, 29, 15, 116, 0, 6);
    read(testDir + "npoess/ExampleFiles/VI1BO_NPP_d2003125_t101038_e10116_b9_c2005829155954_devl_Tst.h5", 0, 6, 12, 43, 0, 4);
    read(testDir + "npoess/ExampleFiles/VI2BO_NPP_d2003125_t101038_e10116_b9_c2005829165919_devl_Tst.h5", 0, 6, 12, 43, 0, 4);
    read(testDir + "npoess/ExampleFiles/VLSTO-GMOFT_NPP_d2003125_t102618_e102646_b9_c2005830184722_de.h5", 0, 23, 16, 106, 0, 6);

    read(testDir + "IASI/IASI_xxx_1C_M02_20070704193256Z_20070704211159Z_N_O_20070704211805Z.h5", 0, 28, 0, 790, 674, 10);
    read(testDir + "IASI/IASI.h5", 0, 28, 0, 777, 661, 10);

    read(testDir + "eos/aura/OMI-Aura_L3-OMTO3e_2005m1214_v002-2006m0929t143855.he5", 2, 4, 19, 28, 0, 7);
    read(testDir + "eos/aura/MLS-Aura_L2GP-BrO_v01-52-c01_2007d029.he5", 5, 27, 6, 152, 0, 11); // */

    // EOS Grids
    read(testDir + "eos/aura/MLS-Aura_L3DM-O3_v02-00-c01_2005d026.he5", 33, 83, 39, 488, 0, 30);
    read(testDir + "eos/aura/TES-Aura_L3-CH4-M2007m07_F01_04.he5", 3, 15, 26, 80, 0, 7);
    read(testDir + "eos/aura/TES-Aura_L3-CH4-M2007m08_F01_04.he5", 3, 15, 26, 80, 0, 7);

    // EOS Swaths
    read(testDir + "eos/aura/OMI-Aura_L2-OMTO3_2007m0624t1616-o15646_v002-2007m0625t152428.he5", 5, 44, 20, 337, 0, 8);

    read(testDir + "eos/HIRDLS/HIR2ARSP_c3_na.he5", 4, 6, 7, 6, 0, 9);
    read(testDir + "eos/HIRDLS/HIRDLS1_v4.0.2a-aIrix-c2_2003d106.he5", 9, 77, 1, 153, 0, 8);
    read(testDir + "eos/HIRDLS/HIRDLS2-AFGL_b027_na.he5", 3, 72, 11, 352, 0, 9);
    read(testDir + "eos/HIRDLS/HIRDLS2-Aura73p_b029_2000d275.he5", 3, 102, 11, 518, 0, 9);
    read(testDir + "eos/HIRDLS/HIRPROF-AFGL_b038_na.he5", 4, 214, 10, 1067, 0, 8);
    read(testDir + "eos/HIRDLS/HIRPROF-Aura73p_b038_2000d275.he5", 4, 214, 10, 1067, 0, 8);
    read(testDir + "eos/HIRDLS/HIRPROF_v582v2.he5", 3, 71, 10, 351, 0, 9);
    read(testDir + "eos/HIRDLS/HIRRAD-Wells-10scans.he5", 5, 38, 9, 40, 0, 9);
  }

  private void read(String filename, int ndims, int nvars, int ngatts, int natts, int nstructFields, int ngroups) throws IOException {
    NetcdfFile ncfile = NetcdfFile.open(filename);
    Counter c = new Counter();
    c.count(ncfile.getRootGroup());

    if (false) {
      filename = filename.substring(testDir.length() + 1);
      filename = StringUtil.replace(filename, '\\', "/");
      System.out.println("read(testDir+\"" + filename + "\"," + c.ndims + "," + c.nvars + "," + c.ngatts + "," + c.natts + "," + c.nstructFields
              + "," + c.ngroups + ");");
      ncfile.close();
      return;
    }

    print(ndims, c.ndims);
    print(nvars, c.nvars);
    print(ngatts, c.ngatts);
    print(natts, c.natts);
    print(nstructFields, c.nstructFields);
    print(ngroups, c.ngroups);
    System.out.println("   " + filename);
    ncfile.close();
  }

  private class Counter {
    int ndims, nvars, natts, ngatts, nstructFields, ngroups;

    private void count(Group g) {
      ndims += g.getDimensions().size();
      nvars += g.getVariables().size();
      ngatts += g.getAttributes().size();
      ngroups += g.getGroups().size();

      for (Variable v : g.getVariables()) {
        natts += v.getAttributes().size();
        if (v instanceof Structure) {
          nstructFields += ((Structure) v).getVariables().size();
        }
      }
      for (Group ng : g.getGroups())
        count(ng);
    }
  }

  private void print(int want, int have) {
    System.out.format("%5d", have);
    System.out.print((want != have) ? "*" : " ");
  }

  public void testAll() throws IOException {
    readAllDir(testDir, new FileFilter() {

      public boolean accept(File pathname) {
        return pathname.getName().endsWith(".h5") || pathname.getName().endsWith(".he5");
      }
    });
  }

  public void readAllDir(String dirName, FileFilter ff) throws IOException {
    System.out.println("---------------Reading directory " + dirName);
    File allDir = new File(dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID " + dirName);
      return;
    }

    for (File f : allFiles) {
      String name = f.getAbsolutePath();
      if (f.isDirectory())
        continue;
      if ((ff == null) || ff.accept(f))
        read(name, 0, 0, 0, 0, 0, 0);
    }

    for (File f : allFiles) {
      if (f.isDirectory())
        readAllDir(f.getAbsolutePath(), ff);
    }

  }

  public void problem() throws IOException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H4header/chunked"));
    NetcdfFile ncfile = NetcdfFile.open(testDir + "eos/aura/MLS-Aura_L3DM-O3_v02-00-c01_2005d026.he5");
    //Variable v = ncfile.findVariable("HDFEOS/GRIDS/OMI Column Amount O3/Data Fields/Reflectivity331");
    //assert v != null;

    //v.read();
  }

}

/*  Results 01/10/08
  dims  vars gatts  atts strFlds groups
    0     1     0     2     0     0    R:/testdata/hdf5/support/astrarr.h5
    0     0     1     0     0     1    R:/testdata/hdf5/support/attstr.h5
    0     4     0     6     0     1    R:/testdata/hdf5/support/bitop.h5
    0     1     0     1     0     0    R:/testdata/hdf5/support/bool.h5
    0     1     0     1     1     0    R:/testdata/hdf5/support/cenum.h5
    0     1     0     1     2     0    R:/testdata/hdf5/support/cstr.h5
    0     1     0     1     3     0    R:/testdata/hdf5/support/cuslab.h5
    0     1     0     1     0     0    R:/testdata/hdf5/support/dstr.h5
    0     1     0     1     0     0    R:/testdata/hdf5/support/dstrarr.h5
    0     1     0     1     2     0    R:/testdata/hdf5/support/DSwith_array_member.h5
    0     1     0     1     0     0    R:/testdata/hdf5/support/enum.h5
    0     1     0     1     0     0    R:/testdata/hdf5/support/SDS_array_type.h5
    0     1     0     1     0     0    R:/testdata/hdf5/support/short.h5
    0     1     0     1     0     0    R:/testdata/hdf5/support/time.h5
    0     1     0     1     0     0    R:/testdata/hdf5/support/uvlstr.h5
    0     1     0     1     0     0    R:/testdata/hdf5/support/vlslab.h5
    0     0     1     0     0     0    R:/testdata/hdf5/support/vlstra.h5
    0     1     0     1     0     1    R:/testdata/hdf5/support/zip.h5
    0     1     0     1     0     0    R:/testdata/hdf5/samples/bitfield.h5
    0     1     0     1     0     0    R:/testdata/hdf5/samples/enum.h5
    0     1     0     1     1     0    R:/testdata/hdf5/samples/enumcmpnd.h5
    0     1     0     1     0     0    R:/testdata/hdf5/samples/f32be.h5
    0     1     0     1     0     0    R:/testdata/hdf5/samples/f32le.h5
    0     1     0     1     0     0    R:/testdata/hdf5/samples/f64be.h5
    0     1     0     1     0     0    R:/testdata/hdf5/samples/f64le.h5
    0     1     0     1     0     0    R:/testdata/hdf5/samples/i16be.h5
    0     1     0     1     0     0    R:/testdata/hdf5/samples/i16le.h5
    0     1     0     1     0     0    R:/testdata/hdf5/samples/i32be.h5
    0     1     0     1     0     0    R:/testdata/hdf5/samples/i32le.h5
    0     1     0     1     0     0    R:/testdata/hdf5/samples/i64be.h5
    0     1     0     1     0     0    R:/testdata/hdf5/samples/i64le.h5
    0     1     0     1     0     0    R:/testdata/hdf5/samples/i8be.h5
    0     1     0     1     0     0    R:/testdata/hdf5/samples/i8le.h5
    0     1     0     1     0     0    R:/testdata/hdf5/samples/il32be.h5
    0     1     0     1     0     0    R:/testdata/hdf5/samples/il32le.h5
    0     1     0     2     0     0    R:/testdata/hdf5/samples/opaque.h5
    0     1     0     1     0     0    R:/testdata/hdf5/samples/strings.h5
    0     1     0     2     0     0    R:/testdata/hdf5/samples/u16be.h5
    0     1     0     2     0     0    R:/testdata/hdf5/samples/u16le.h5
    0     1     0     2     0     0    R:/testdata/hdf5/samples/u32be.h5
    0     1     0     2     0     0    R:/testdata/hdf5/samples/u32le.h5
    0     1     0     2     0     0    R:/testdata/hdf5/samples/u64be.h5
    0     1     0     2     0     0    R:/testdata/hdf5/samples/u64le.h5
    0     1     0     2     0     0    R:/testdata/hdf5/samples/u8be.h5
    0     1     0     2     0     0    R:/testdata/hdf5/samples/u8le.h5
    0     1     0     2     0     0    R:/testdata/hdf5/samples/ul32be.h5
    0     1     0     2     0     0    R:/testdata/hdf5/samples/ul32le.h5
    0     1     0     1     7     0    R:/testdata/hdf5/complex/compound_complex.h5
    0     1     0     1    12     0    R:/testdata/hdf5/complex/compound_native.h5

  dims  vars gatts  atts strFlds groups
    0    57   157   204     0    60    R:/testdata/hdf5/msg/test.h5
    0    50    44   338     3     3    R:/testdata/hdf5/wrf/wrf_bdy_par.h5
    0   110    43   758     3     3    R:/testdata/hdf5/wrf/wrf_input_par.h5
    0    73    43   499     3     3    R:/testdata/hdf5/wrf/wrf_out_par.h5
    0    50    44   338     3     2    R:/testdata/hdf5/wrf/wrf_bdy_seq.h5
    0   110    43   758     3     2    R:/testdata/hdf5/wrf/wrf_input_seq.h5
    0    73    43   499     3     2    R:/testdata/hdf5/wrf/wrf_out_seq.h5
    0     7    12    51     0     4    R:/testdata/hdf5/npoess/ExampleFiles/AVAFO_NPP_d2003125_t10109_e101038_b9_c2005829155458_devl_Tst.h5
    0    24    16   104     0     6    R:/testdata/hdf5/npoess/ExampleFiles/GDNBF-VNCCO_NPP_d2003125_t101038_e10116_b9_c2005829162517_dev.h5
    0    20    16    98     0     6    R:/testdata/hdf5/npoess/ExampleFiles/GIMFT-VIVIO_NPP_d2003125_t101038_e10116_b9_c2005829173243_dev.h5
    0    41    16   134     0     6    R:/testdata/hdf5/npoess/ExampleFiles/GMOFT-VOCCO_NPP_d2003125_t101038_e10116_b9_c2005829163632_dev.h5
    0    25    16   109     0     6    R:/testdata/hdf5/npoess/ExampleFiles/GMOFT-VSSTO_NPP_d2003125_t101038_e10116_b9_c2005829165313_dev.h5
    0     3     8    26     0     4    R:/testdata/hdf5/npoess/ExampleFiles/RVIRS_NPP_d2003125_t101038_e10116_b9_c200582917556_devl_Tst.h5
    0    29    15   116     0     6    R:/testdata/hdf5/npoess/ExampleFiles/SVI01-GIMFG_NPP_d2003125_t101038_e10116_b9_c2005829153351_dev.h5
    0    29    15   116     0     6    R:/testdata/hdf5/npoess/ExampleFiles/SVM04-GMOFG_NPP_d2003125_t101038_e10116_b9_c2005829171444_dev.h5
    0     6    12    43     0     4    R:/testdata/hdf5/npoess/ExampleFiles/VI1BO_NPP_d2003125_t101038_e10116_b9_c2005829155954_devl_Tst.h5
    0     6    12    43     0     4    R:/testdata/hdf5/npoess/ExampleFiles/VI2BO_NPP_d2003125_t101038_e10116_b9_c2005829165919_devl_Tst.h5
    0    23    16   106     0     6    R:/testdata/hdf5/npoess/ExampleFiles/VLSTO-GMOFT_NPP_d2003125_t102618_e102646_b9_c2005830184722_de.h5
    0    28     0   790   674    10    R:/testdata/hdf5/IASI/IASI_xxx_1C_M02_20070704193256Z_20070704211159Z_N_O_20070704211805Z.h5
    0    28     0   777   661    10    R:/testdata/hdf5/IASI/IASI.h5

  dims  vars gatts  atts strFlds groups
    0     4    19    28     0     7    R:/testdata/hdf5/eos/aura/OMI-Aura_L3-OMTO3e_2005m1214_v002-2006m0929t143855.he5
    3    27     6   152     0    11    R:/testdata/hdf5/eos/aura/MLS-Aura_L2GP-BrO_v01-52-c01_2007d029.he5
    4    83    39   488     0    30    R:/testdata/hdf5/eos/aura/MLS-Aura_L3DM-O3_v02-00-c01_2005d026.he5
    5    44    20   337     0     8    R:/testdata/hdf5/eos/aura/OMI-Aura_L2-OMTO3_2007m0624t1616-o15646_v002-2007m0625t152428.he5
    0    15    26    80     0     7    R:/testdata/hdf5/eos/aura/oon/TES-Aura_L3-CH4-M2007m07_F01_04.he5
    0    15    26    80     0     7    R:/testdata/hdf5/eos/aura/oon/TES-Aura_L3-CH4-M2007m08_F01_04.he5
    4     6     7     6     0     9    R:/testdata/hdf5/eos/HIRDLS/HIR2ARSP_c3_na.he5
    9    77     1   153     0     8    R:/testdata/hdf5/eos/HIRDLS/HIRDLS1_v4.0.2a-aIrix-c2_2003d106.he5
    3    72    11   352     0     9    R:/testdata/hdf5/eos/HIRDLS/HIRDLS2-AFGL_b027_na.he5
    3   102    11   518     0     9    R:/testdata/hdf5/eos/HIRDLS/HIRDLS2-Aura73p_b029_2000d275.he5
    4   214    10  1067     0     8    R:/testdata/hdf5/eos/HIRDLS/HIRPROF-AFGL_b038_na.he5
    4   214    10  1067     0     8    R:/testdata/hdf5/eos/HIRDLS/HIRPROF-Aura73p_b038_2000d275.he5
    3    71    10   351     0     9    R:/testdata/hdf5/eos/HIRDLS/HIRPROF_v582v2.he5
    5    38     9    40     0     9    R:/testdata/hdf5/eos/HIRDLS/HIRRAD-Wells-10scans.he5

 */

