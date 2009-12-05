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
package ucar.nc2.iosp.bufr;

import ucar.nc2.iosp.bufr.tables.*;

import java.util.*;
import java.io.IOException;

/**
 * Encapsolates lookup into the BUFR Tables.
 *
 * @author caron
 * @since Jul 14, 2008
 */
public final class TableLookup {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TableLookup.class);

  public enum Mode {
    wmoOnly,        // wmo entries only found from wmo table
    wmoLocal,       // if wmo entries not found in wmo table, look in local table
    localOverride   // look in local first, then wmo
  }

  private static TableA tablelookup;
  private static TableA wmoTableA;
  //private static TableB wmoTableB;
  //private static TableD wmoTableD;
  // private static String wmoTableName;

  static private void init() {
    if (tablelookup != null) return;
    try {
      tablelookup = BufrTables.getLookupTable();
      wmoTableA = BufrTables.getWmoTableA();
      //wmoTableB = BufrTables.getWmoTableB();
      //wmoTableD = BufrTables.getWmoTableD();
    } catch (IOException ioe) {
      log.error("Filed to read BUFR table ", ioe);
    }
  }
  static private final boolean showErrors = true;

  /////////////////////////////////////////
  private final String localTableName;
  private TableB localTableB;
  private TableD localTableD;

  private TableB wmoTableB;
  private TableD wmoTableD;

  public Mode mode = Mode.wmoOnly;

  public TableLookup(BufrIndicatorSection is, BufrIdentificationSection ids) throws IOException {
    init();
    this.wmoTableB = BufrTables.getWmoTableB(is.getBufrEdition());
    this.wmoTableD = BufrTables.getWmoTableD(is.getBufrEdition());

    // check tablelookup for special local table
    // create key from category and possilbly center id
    localTableName = tablelookup.getCategory( makeLookupKey(ids.getCategory(), ids.getCenterId()));

    if (localTableName == null) {
      this.localTableB = BufrTables.getWmoTableB(is.getBufrEdition());
      this.localTableD = BufrTables.getWmoTableD(is.getBufrEdition());
      return;

    } else if (localTableName.contains("-ABD")) {
      localTableB = BufrTables.getTableB(localTableName.replace("-ABD", "-B"));
      localTableD = BufrTables.getTableD(localTableName.replace("-ABD", "-D"));
      return;

      // check if localTableName(Mnemonic) table has already been processed
    } else if (BufrTables.hasTableB(localTableName)) {
      localTableB = BufrTables.getTableB(localTableName); // LOOK localTableName needs "-B" or something
      localTableD = BufrTables.getTableD(localTableName);
      return;
    }

    // Mnemonic tables
    ucar.nc2.iosp.bufr.tables.BufrReadMnemonic brm = new ucar.nc2.iosp.bufr.tables.BufrReadMnemonic();
    brm.readMnemonic(localTableName);
    this.localTableB = brm.getTableB();
    this.localTableD = brm.getTableD();
  }

  private short makeLookupKey(int cat, int center_id) {
    if ((center_id == 7) || (center_id == 8) || (center_id == 9)) {
      if (cat < 240 || cat > 254)
        return (short) center_id;
      return (short) (center_id * 1000 + cat);
    }
    return (short) center_id;
  }

  public void setMode(Mode mode) {
    this.mode = mode;
  }

  public Mode getMode() {
    return mode;
  }

  public String getDataCategory(int cat) {
    return wmoTableA.getDataCategory((short) cat);
  }

  public final String getWmoTableName() {
    return "wmo table version 14";
  }

  public final String getLocalTableName() {
    return localTableName;
  }

  public TableB.Descriptor getDescriptorTableB(short fxy) {
    TableB.Descriptor b = null;
    boolean isWmoRange = Descriptor.isWmoRange(fxy);

    if (isWmoRange && mode.equals(Mode.wmoOnly)) {
      b = wmoTableB.getDescriptor(fxy);

    } else if (isWmoRange && mode.equals(Mode.wmoLocal)) {
      b = wmoTableB.getDescriptor(fxy);
      if (b == null)
        b = localTableB.getDescriptor(fxy);

    } else if (isWmoRange && mode.equals(Mode.localOverride)) {
      if (localTableB != null)
        b = localTableB.getDescriptor(fxy);
      if (b == null)
        b = wmoTableB.getDescriptor(fxy);

    } else {  
      if (localTableB != null)
        b = localTableB.getDescriptor(fxy);
    }

    if (b == null && showErrors)
      System.out.println(" TableLookup cant find Table B descriptor =" + Descriptor.makeString(fxy) + " in tables= " + localTableName + "," + wmoTableB.getName());
    return b;
  }

  public List<Short> getDescriptorsTableD(short id) {
    TableD.Descriptor d = getDescriptorTableD(id);
    if (d != null)
      return d.getSequence();
    return null;
  }

  public List<String> getDescriptorsTableD(String fxy) {
    short id = Descriptor.getFxy(fxy);
    List<Short> seq = getDescriptorsTableD(id);
    if (seq == null) return null;
    List<String> result = new ArrayList<String>( seq.size());
    for (Short s : seq)
      result.add( Descriptor.makeString(s));
    return result;
  }
  
  public TableD.Descriptor getDescriptorTableD(short fxy) {
    TableD.Descriptor d = null;
    boolean isWmoRange = Descriptor.isWmoRange(fxy);

    if (isWmoRange && mode.equals(Mode.wmoOnly)) {
      d = wmoTableD.getDescriptor(fxy);

    } else if (isWmoRange && mode.equals(Mode.wmoLocal)) {
      d = wmoTableD.getDescriptor(fxy);
      if (d == null)
        d = localTableD.getDescriptor(fxy);

    } else if (isWmoRange && mode.equals(Mode.localOverride)) {
      if (localTableD != null)
        d = localTableD.getDescriptor(fxy);
      if (d == null)
        d = wmoTableD.getDescriptor(fxy);

    } else {
      if (localTableD != null)
        d = localTableD.getDescriptor(fxy);
    }

    if (d == null && showErrors)
      System.out.printf(" TableLookup cant find Table D descriptor %s in tables %s,%s%n", Descriptor.makeString(fxy), localTableName, wmoTableD.getName());
    return d;
  }

}
