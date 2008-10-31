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
package ucar.nc2.iosp.bufr;

import ucar.nc2.iosp.bufr.tables.*;

import java.util.*;
import java.io.IOException;

/**
 * Encapsolates lookup into the BUFR Tables.
 *
 * @author caron
 *         modified by rkambic
 * @since Jul 14, 2008
 */
public final class TableLookup {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TableLookup.class);

  public enum Mode {
    wmoOnly,        // wmo entries only found from wmo table
    wmoLocal,       // if wmo entries not found in  wmo table, look in local table
    localOverride   // look in local first, then wmo
  }

  private static TableA tablelookup;
  private static TableA wmoTableA;
  private static TableB wmoTableB;
  private static TableD wmoTableD;
  private static String wmoTableName;

  static {
    try {
      tablelookup = BufrTables.getTableA("tablelookup.txt");
      // get wmo tables
      wmoTableName = tablelookup.getDataCategory((short) 0);

      wmoTableA = BufrTables.getTableA(wmoTableName + "-A");
      wmoTableB = BufrTables.getTableB(wmoTableName + "-B");
      wmoTableD = BufrTables.getTableD(wmoTableName + "-D");
    } catch (IOException ioe) {
      log.error("Filed to read BUFR table ", ioe);
    }
  }
  static private final boolean showErrors = false;

  /////////////////////////////////////////
  private final String localTableName;
  private TableB localTableB;
  private TableD localTableD;

  public Mode mode = Mode.wmoOnly;

  public TableLookup(BufrIndicatorSection is, BufrIdentificationSection ids) throws IOException {

    // check tablelookup for special local table
    // create key from category and possilbly center id
    localTableName = tablelookup.getCategory( makeLookupKey(ids.getCategory(), ids.getCenterId()));

    if (localTableName == null) {
      this.localTableB = wmoTableB;
      this.localTableD = wmoTableD;
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

  public String getSubCategory(int cat, int subCat) {
    return TableDataSubcategories.getSubCategory(cat, subCat);
  }

  public final String getWmoTableName() {
    return wmoTableName;
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
      System.out.println("Cant find Table B descriptor =" + Descriptor.makeString(fxy) + " in tables= " + localTableName + "," + wmoTableName);
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
      System.out.println("Cant find Table D descriptor =" + Descriptor.makeString(fxy) + " in tables= " + localTableName + "," + wmoTableName);
    return d;
  }

////////////////////////////////////////////////////////

  /*    public static void main(String args[]) throws IOException {

// Function References
String tableName;
if (args.length == 1) {
  tableName = args[0];
} else {
  tableName = "B4L-046-013-ABD.diff";
  tableName = "bufrtab.ETACLS1";
}

TableLookup tlu = new TableLookup(  tableName );

//short test = tlu.makeLookupKey( 243, 46 );
//String ltn = tablelookup.getDataCategory( test);

// only can be element descriptors in tableB
//
TableBdescriptor b;
Vector<Short> v = new Vector( tlu.localTableB.getMap().keySet());
Collections.sort( v );
System.out.println( "WMO table "+  tlu.getWmoTableName() );
System.out.println( "Local table "+ tlu.getLocalTableName().replace( "-ABD", "-B"));
System.out.println();
System.out.println("Table B Descriptors:");
System.out.println();
for ( Short id : v ) {
    b = tlu.getDescriptorTableB( id );
    if ( b == null && tlu.mode.equals( Mode.wmoOnly )) {
        System.out.println("fxy ="+ BufrDataDescriptionSection.getDescName(id) +" not in wmoTable "+ tlu.getWmoTableName() );
        continue;
    }
    int x = (id & 0x3F00) >> 8;
    int y = id & 0xFF;
    boolean WMOrange = ( x > 47 || y > 191 ? false : true );
    if ( WMOrange && ! b.isWMO() )
       System.out.print("***Warning*** fxy in wmoRange found in local table "+ tlu.getLocalTableName() +"   ");
    System.out.println("fxy ="+ b.getFxy() +" name ="+ b.getName() +" isWMO ="+ b.isWMO());
}
// sequences can be other sequences as well as element descriptors
System.out.println();

List<String> al;
TableDdescriptor d;
v = new Vector( tlu.localTableD.getMap().keySet());
Collections.sort( v );
System.out.println("Table D Descriptors:\n");
for ( Short id : v ) {
    d = tlu.getDescriptorTableD( id );
    if ( d == null && tlu.mode.equals( Mode.wmoOnly )) {
        System.out.println("fxy ="+ BufrDataDescriptionSection.getDescName(id) +" not in wmoTable "+ tlu.getWmoTableName() );
        continue;
    }
    int x = (id & 0x3F00) >> 8;
    int y = id & 0xFF;
    boolean WMOrange = ( x > 47 || y > 191 ? false : true );
    if ( WMOrange && ! d.isWMO() )
       System.out.print("***Warning*** fxy in wmoRange found in local table "+ tlu.getLocalTableName() +"   ");
    al = d.getDescList();
    System.out.println("fxy "+ BufrDataDescriptionSection.getDescName(id) +" isWMO ="+ d.isWMO());
    System.out.println("  list =" + al);
}
}    */

}