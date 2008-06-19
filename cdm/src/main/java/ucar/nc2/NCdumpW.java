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
package ucar.nc2;

import ucar.ma2.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.URLnaming;
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.nio.charset.Charset;

/**
 * Print contents of an existing netCDF file, using a Writer.
 *
 * A difference with ncdump is that the nesting of multidimensional array data is represented by nested brackets,
 * so the output is not legal CDL that can be used as input for ncgen. Also, the default is header only (-h)
 *
 * @author caron
 * @since Nov 4, 2007
 */

public class NCdumpW {
  private static String usage = "usage: NCdumpW <filename> [-cdl | -ncml] [-c | -vall] [-v varName1;varName2;..] [-v varName(0:1,:,12)]\n";

  /**
   * Print netcdf "header only" in CDL.
   * @param fileName open this file
   * @param out print to this Writer
   * @return true if successful
   * @throws java.io.IOException on write error
   */
  public static boolean printHeader(String fileName, Writer out) throws java.io.IOException {
    return print( fileName, out, false, false, false, false, null, null);
  }

  /**
   * print NcML representation of this netcdf file, showing coordinate variable data.
   * @param fileName open this file
   * @param out print to this Writer
   * @return true if successful
   * @throws java.io.IOException on write error
   */
  public static boolean printNcML(String fileName, Writer out) throws java.io.IOException {
    return print( fileName, out, false, true, true, false, null, null);
  }

   /**
   * NCdump that parses a command string, using default options.
   * Usage:
   * <pre>NCdump filename [-ncml] [-c | -vall] [-v varName;...]</pre>
   * @param command command string
   * @param out send output here
   * @return true if successful
   * @throws java.io.IOException on write error
   */
  public static boolean print(String command, Writer out) throws java.io.IOException {
    return print( command, out, null);
  }

  /**
   * ncdump that parses a command string.
   * Usage:
   * <pre>NCdump filename [-ncml] [-c | -vall] [-v varName;...]</pre>
   * @param command command string
   * @param out send output here
   * @param ct allow task to be cancelled; may be null.
   * @return true if successful
   * @throws java.io.IOException on write error
   */
  public static boolean print(String command, Writer out, ucar.nc2.util.CancelTask ct) throws java.io.IOException {

    // pull out the filename from the command
    String filename;
    StringTokenizer stoke = new StringTokenizer( command);
    if (stoke.hasMoreTokens())
      filename = stoke.nextToken();
    else {
      out.write( usage);
      return false;
    }

    NetcdfFile nc = null;
    try {
      nc = NetcdfFile.open(filename, ct);

      // the rest of the command
      int pos = command.indexOf(filename);
      command = command.substring(pos + filename.length());
      return print(nc, command, out, ct);

    } catch (java.io.FileNotFoundException e) {
      out.write( "file not found= ");
      out.write( filename);
      return false;

    } finally {
      if (nc != null) nc.close();
      out.flush();
    }

  }

  /**
   * ncdump, parsing command string, file already open.
   * @param nc apply command to this file
   * @param command : command string
   * @param out send output here
   * @param ct allow task to be cancelled; may be null.
   * @return true if successful
   * @throws java.io.IOException on write error
   */
  public static boolean print(NetcdfFile nc, String command, Writer out, ucar.nc2.util.CancelTask ct) throws java.io.IOException {

    boolean showAll = false;
    boolean showCoords = false;
    boolean ncml = false;
    boolean strict = false;
    String varNames = null;

    if (command != null) {
      StringTokenizer stoke = new StringTokenizer( command);

      while (stoke.hasMoreTokens()) {
        String toke = stoke.nextToken();
         if (toke.equalsIgnoreCase("-help")) {
          out.write( usage);
          out.write( '\n');
          return true;
        }
        if (toke.equalsIgnoreCase("-vall"))
          showAll = true;
        if (toke.equalsIgnoreCase("-c"))
          showCoords = true;
        if (toke.equalsIgnoreCase("-ncml"))
          ncml = true;
        if (toke.equalsIgnoreCase("-cdl"))
          strict = true;
        if (toke.equalsIgnoreCase("-v") && stoke.hasMoreTokens())
          varNames = stoke.nextToken();
      }
    }

    return print( nc, out, showAll, showCoords, ncml, strict, varNames, ct);
  }

  /**
   *  ncdump-like print of netcdf file.
   * @param filename NetcdfFile to open
   * @param out print to this stream
   * @param showAll dump all variable data
   * @param showCoords only print header and coordinate variables
   * @param ncml print NcML representation (other arguments are ignored)
   * @param strict print strict CDL representation
   * @param varNames semicolon delimited list of variables whose data should be printed
   * @param ct allow task to be cancelled; may be null.
   * @return true if successful
   * @throws java.io.IOException on write error

   */
  public static boolean print(String filename, Writer out, boolean showAll, boolean showCoords,
    boolean ncml, boolean strict, String varNames, ucar.nc2.util.CancelTask ct) throws java.io.IOException {

    NetcdfFile nc = null;
    try {
      //nc = NetcdfFileCache.acquire(fileName, ct);
      nc = NetcdfFile.open(filename, ct);
      return print(nc, out, showAll, showCoords, ncml, strict, varNames, ct);

    } catch (java.io.FileNotFoundException e) {
      out.write( "file not found= ");
      out.write( filename);
      out.flush();
      return false;

    } finally {
      if (nc != null) nc.close();
    }

  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////
  // heres where the work is done

  /**
   *  ncdump-like print of netcdf file.
   * @param nc already opened NetcdfFile
   * @param out print to this stream
   * @param showAll dump all variable data
   * @param showCoords only print header and coordinate variables
   * @param ncml print NcML representation (other arguments are ignored)
   * @param strict print strict CDL representation
   * @param varNames semicolon delimited list of variables whose data should be printed. May have
   *  Fortran90 like selector: eg varName(1:2,*,2)
   * @param ct allow task to be cancelled; may be null.
   * @return true if successful
   * @throws java.io.IOException on write error
   */
  public static boolean print(NetcdfFile nc, Writer out, boolean showAll, boolean showCoords,
      boolean ncml, boolean strict, String varNames, ucar.nc2.util.CancelTask ct) throws java.io.IOException {

    boolean headerOnly = !showAll && (varNames == null);

    try {

      if (ncml)
        writeNcML(nc, out, showCoords, null); // output schema in NcML
      else if (headerOnly)
        nc.writeCDL(new PrintWriter( out), strict); // output schema in CDL form (like ncdump)
      else {
        PrintWriter ps = new PrintWriter( out);
        nc.toStringStart(ps, strict);
        ps.print(" data:\n");

        if (showAll) { // dump all data
          for (Variable v : nc.getVariables()) {
            printArray(v.read(), v.getName(), ps, ct);
            if (ct != null && ct.isCancel()) return false;
          }
        }

        else if (showCoords) { // dump coordVars
          for (Variable v : nc.getVariables()) {
            if (v.isCoordinateVariable())
              printArray(v.read(), v.getName(), ps, ct);
            if (ct != null && ct.isCancel()) return false;
          }
        }

        if (!showAll && (varNames != null)) { // dump the list of variables
          StringTokenizer stoke = new StringTokenizer(varNames,";");
          while (stoke.hasMoreTokens()) {
            String varSubset = stoke.nextToken(); // variable name and optionally a subset

            if (varSubset.indexOf('(') >= 0) { // has a selector
              Array data = nc.readSection(varSubset);
              printArray(data, varSubset, ps, ct);

            } else {   // do entire variable
              Variable v = nc.findVariable(varSubset);
              if (v == null) {
                ps.print(" cant find variable: "+varSubset+"\n   "+usage);
                continue;
              }
              // dont print coord vars if they are already printed
              if (!showCoords || v.isCoordinateVariable())
                printArray( v.read(), v.getName(), ps, ct);
            }
            if (ct != null && ct.isCancel()) return false;
          }
        }

        nc.toStringEnd( ps);
      }

    } catch (Exception e) {
      e.printStackTrace();
      out.write(e.getMessage());
      out.flush();
      return false;
    }

    out.flush();
    return true;
  }


  /**
   * Print all the data of the given Variable.
   * @param v variable to print
   * @param ct allow task to be cancelled; may be null.
   * @return String result
   * @throws java.io.IOException on write error
   */
  static public String printVariableData(VariableIF v, ucar.nc2.util.CancelTask ct) throws IOException {
    Array data = v.read();
    /* try {
      data = v.isMemberOfStructure() ? v.readAllStructures(null, true) : v.read();
    }
    catch (InvalidRangeException ex) {
      return ex.getMessage();
    } */

    StringWriter writer = new StringWriter(10000);
    printArray( data, v.getName(), new PrintWriter(writer), ct);
    return writer.toString();
  }

  /**
   * Print a section of the data of the given Variable.
   * @param v variable to print
   * @param sectionSpec string specification
   * @param ct allow task to be cancelled; may be null.
   * @return String result formatted data ouptut
   * @throws IOException on write error
   * @throws InvalidRangeException is specified section doesnt match variable shape
   */
  static public String printVariableDataSection(Variable v, String sectionSpec, ucar.nc2.util.CancelTask ct) throws IOException, InvalidRangeException {
    Array data = v.read(sectionSpec);

    StringWriter writer = new StringWriter(20000);
    printArray( data, v.getName(), new PrintWriter( writer), ct);
    return writer.toString();
  }

  /**
   * Print the data array.
   * @param array data to print.
   * @param name title the output.
   * @param out send output here.
   * @param ct allow task to be cancelled; may be null.
   * @throws java.io.IOException on read error
   */
  static public void printArray(Array array, String name, PrintWriter out, CancelTask ct) throws IOException {
    printArray( array, name, null, out, new Indent(2), ct);
    out.flush();
  }

  static public String printArray(Array array, String name, CancelTask ct) throws IOException {
    CharArrayWriter carray = new CharArrayWriter(100000);
    PrintWriter pw = new PrintWriter( carray);
    printArray( array, name, null, pw, new Indent(2), ct);
    return carray.toString();
  }

  static private void printArray(Array array, String name, String units, PrintWriter out, Indent ilev, CancelTask ct) throws IOException {
    if (ct != null && ct.isCancel()) return;

    if (name != null) out.print(ilev+name + " =");
    ilev.incr();

    if (array == null)
      throw new IllegalArgumentException("null array");

    if ((array instanceof ArrayChar) && (array.getRank() > 0) ) {
      printStringArray(out, (ArrayChar) array, ilev, ct);

    } else if (array.getElementType() == String.class) {
      printStringArray(out, (ArrayObject) array, ilev, ct);

    } else if (array.getElementType() == StructureData.class) {
      if (array.getSize() == 1)
        printStructureData( out, (StructureData) array.getObject( array.getIndex()), ilev, ct);
      else
        printStructureDataArray( out, (ArrayStructure) array, ilev, ct);

    } else if (array.getElementType() == StructureDataIterator.class) {
      printSequence( out, (ArraySequence) array, ilev, ct);

     } else {
      printArray(array, out, ilev, ct);
    }

    if (units != null)
      out.print(" "+units);
    out.print("\n");
    ilev.decr();
    out.flush();
  }

  static private void printArray(Array ma, PrintWriter out, Indent indent, CancelTask ct) {
     if (ct != null && ct.isCancel()) return;

    int rank = ma.getRank();
    Index ima = ma.getIndex();

    // scalar
    if (rank == 0) {
      out.print( ma.getObject(ima).toString());
      return;
    }

    int [] dims = ma.getShape();
    int last = dims[0];

    out.print("\n" + indent + "{");

    if ((rank == 1) && (ma.getElementType() != StructureData.class)) {
      for(int ii = 0; ii < last; ii++) {
        out.print( ma.getObject(ima.set(ii)).toString());
        if (ii != last-1) out.print(", ");
        if (ct != null && ct.isCancel()) return;
      }
      out.print("}");
      return;
    }

    indent.incr();
    for(int ii = 0; ii < last; ii++) {
      Array slice = ma.slice(0, ii);
      printArray(slice, out, indent, ct);
      if(ii != last-1) out.print(",");
      if (ct != null && ct.isCancel()) return;
    }
    indent.decr();

    out.print("\n"+indent + "}");
  }

  static void printStringArray(PrintWriter out, ArrayChar ma, Indent indent, ucar.nc2.util.CancelTask ct) {
    if (ct != null && ct.isCancel()) return;

    int rank = ma.getRank();

    if (rank == 1) {
      out.print( "  \""+ma.getString()+"\"");
      return;
    }

    if (rank == 2) {
      boolean first = true;
      for (ArrayChar.StringIterator iter = ma.getStringIterator(); iter.hasNext(); ) {
        if (!first) out.print(", ");
        out.print( "\""+iter.next()+"\"");
        first = false;
        if (ct != null && ct.isCancel()) return;
      }
      return;
    }

    int [] dims = ma.getShape();
    int last = dims[0];

    out.print("\n" + indent + "{");
    indent.incr();
    for(int ii = 0; ii < last; ii++) {
      ArrayChar slice = (ArrayChar) ma.slice(0, ii);
      printStringArray(out, slice, indent, ct);
      if(ii != last-1) out.print(",");
      if (ct != null && ct.isCancel()) return;
    }
    indent.decr();

    out.print("\n"+indent + "}");
  }

  static void printStringArray(PrintWriter out, ArrayObject ma, Indent indent, ucar.nc2.util.CancelTask ct) {
    if (ct != null && ct.isCancel()) return;

    int rank = ma.getRank();
    Index ima = ma.getIndex();

    if (rank == 0) {
      out.print( "  \""+ma.getObject(ima)+"\"");
      return;
    }

    if (rank == 1) {
      boolean first = true;
      for (int i=0; i<ma.getSize(); i++) {
        if (!first) out.print(", ");
        out.print( "  \""+ma.getObject(ima.set(i))+"\"");
        first = false;
      }
      return;
    }

    int [] dims = ma.getShape();
    int last = dims[0];

    out.print("\n" + indent + "{");
    indent.incr();
    for(int ii = 0; ii < last; ii++) {
      ArrayObject slice = (ArrayObject) ma.slice(0, ii);
      printStringArray(out, slice, indent, ct);
      if(ii != last-1) out.print(",");
      //out.print("\n");
    }
    indent.decr();

    out.print("\n"+indent+ "}");
  }

  static private void printStructureDataArray(PrintWriter out, ArrayStructure array, Indent indent,
                                              ucar.nc2.util.CancelTask ct) throws IOException {
    StructureDataIterator sdataIter = array.getStructureDataIterator();
    int count = 0;
    while (sdataIter.hasNext()) {
      StructureData sdata = sdataIter.next();
      out.println("\n" + indent + "{");
      printStructureData( out, sdata, indent, ct);
      //ilev.setIndentLevel(saveIndent);
      out.print(indent+ "} "+sdata.getName()+"("+count+")");
      if (ct != null && ct.isCancel()) return;
      count++;
    }
  }

  static private void printSequence(PrintWriter out, ArraySequence seq, Indent indent,  CancelTask ct) throws IOException {
    StructureDataIterator iter = seq.getStructureDataIterator();
    while (iter.hasNext()) {
      StructureData sdata = iter.next();
      out.println("\n" + indent + "{");
      printStructureData( out, sdata, indent, ct);
      out.print(indent+ "} "+sdata.getName());
      if (ct != null && ct.isCancel()) return;
    }
  }

  /**
   * Print contents of a StructureData.
   * @param out send output here.
   * @param  sdata StructureData to print.
   * @throws java.io.IOException on read error
   */
  static public void printStructureData(PrintWriter out, StructureData sdata) throws IOException {
    printStructureData(out, sdata, new Indent(2), null);
    out.flush();
  }

  static private void printStructureData(PrintWriter out, StructureData sdata, Indent indent, CancelTask ct) throws IOException {
    indent.incr();
    //int saveIndent = ilev.getIndentLevel();
    for (StructureMembers.Member m : sdata.getMembers()) {
      Array sdataArray = sdata.getArray(m);
      //ilev.setIndentLevel(saveIndent);
      printArray(sdataArray, m.getName(), m.getUnitsString(), out, indent, ct);
      if (ct != null && ct.isCancel()) return;
    }
    indent.decr();
  }

  /**
   * Maintains indentation level for printing nested structures.
   */
  private static class Indent {
    private int nspaces = 0;
    private int level = 0;
    private StringBuilder blanks;
    private String indent = "";

    // nspaces = how many spaces each level adds.
    // max 100 levels
    public Indent(int nspaces) {
      this.nspaces = nspaces;
      blanks = new StringBuilder();
      for (int i=0; i < 100*nspaces; i++)
        blanks.append(" ");
    }

    public Indent incr() {
      level++;
      setIndentLevel( level);
      return this;
    }

    public Indent decr() {
      level--;
      setIndentLevel( level);
      return this;
    }

    public String toString() { return indent; }

    public void setIndentLevel(int level) {
      this.level = level;
      indent = blanks.substring(0, level * nspaces);
    }
  }

  /**
   * Print array as undifferentiated sequence of values.
   * @param ma any Array except ArrayStructure
   * @param out print to here
   */
  static public void printArray(Array ma, PrintWriter out) {
    while (ma.hasNext()) {
      out.print( ma.next());
      out.print( ' ');
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // standard NCML writing.

  /**
   * Write the NcML representation for a file.
   * Note that ucar.nc2.dataset.NcMLWriter has a JDOM implementation, for complete NcML.
   * This method implements only the "core" NcML for plain ole netcdf files.
   *
   * @param ncfile write NcML for this file
   * @param os write to this Writer. Must be using UTF-8 encoding (where applicable)
   * @param showCoords show coordinate variable values.
   * @param uri use this for the uri attribute; if null use getLocation(). // ??
   * @throws IOException on write error
   */
  static public void writeNcML( NetcdfFile ncfile, java.io.Writer os, boolean showCoords, String uri) throws IOException {
     PrintWriter out = new PrintWriter( os);
     out.print("<?xml version='1.0' encoding='UTF-8'?>\n");
     out.print("<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'\n");

    // out.print("    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n");
    // out.print("    xsi:schemaLocation='http://www.unidata.edu/schemas/ncml-2.2 http://www.unidata.ucar.edu/schemas/ncml-2.2.xsd'\n");

    if (uri != null)
      out.print("    location='"+ StringUtil.quoteXmlAttribute(uri)+"' >\n\n");
    else
      out.print("    location='"+ StringUtil.quoteXmlAttribute( URLnaming.canonicalizeWrite(ncfile.getLocation()))+"' >\n\n");

    if (ncfile.getId() != null)
      out.print("    id='"+ StringUtil.quoteXmlAttribute(ncfile.getId())+"' >\n");
    if (ncfile.getTitle() != null)
      out.print("    title='"+ StringUtil.quoteXmlAttribute(ncfile.getTitle())+"' >\n");

    writeNcMLGroup( ncfile, ncfile.getRootGroup(), out, new Indent(2), showCoords);

    out.print("</netcdf>\n");
    out.flush();
  }

  static private void writeNcMLGroup( NetcdfFile ncfile, Group g, PrintWriter out, Indent indent, boolean showCoords) throws IOException {
    if (g != ncfile.getRootGroup()) {
      out.print(indent);
      out.print("<group name='" + StringUtil.quoteXmlAttribute(g.getShortName()) + "' >\n");
    }
    indent.incr();

    List<Dimension> dimList = g.getDimensions();
    for (Dimension dim : dimList) {
      out.print(indent);
      out.print("<dimension name='" + StringUtil.quoteXmlAttribute(dim.getName()) + "' length='" + dim.getLength() + "'");
      if (dim.isUnlimited())
        out.print(" isUnlimited='true'");
      out.print(" />\n");
    }
    if (dimList.size() > 0)
      out.print("\n");

    List<Attribute> attList = g.getAttributes();
    for (Attribute att : attList) {
      writeNcMLAtt(att, out, indent);
    }
    if (attList.size() > 0)
      out.print("\n");

    for (Variable v : g.getVariables()) {
      if (v instanceof Structure) {
        writeNcMLStructure((Structure) v, out, indent);
      } else {
        writeNcMLVariable(v, out, indent, showCoords);
      }
    }

    // nested groups
    List groupList = g.getGroups();
    for (int i=0; i<groupList.size(); i++) {
      if (i > 0) out.print("\n");
      Group nested = (Group) groupList.get(i);
      writeNcMLGroup( ncfile, nested, out, indent, showCoords);
    }

    indent.decr();

    if (g != ncfile.getRootGroup()) {
      out.print(indent);
      out.print("</group>\n");
    }
  }

  static private void writeNcMLStructure( Structure s, PrintWriter out, Indent indent) throws IOException {
    out.print(indent);
    out.print("<structure name='"+StringUtil.quoteXmlAttribute(s.getShortName()));

    // any dimensions?
    if (s.getRank() > 0) {
      writeNcMLDimension( s, out);
    }
    out.print(">\n");

    indent.incr();

    List<Attribute> attList = s.getAttributes();
    for (Attribute att : attList) {
      writeNcMLAtt(att, out, indent);
    }
    if (attList.size() > 0)
      out.print("\n");

    List<Variable> varList = s.getVariables();
    for (Variable v : varList) {
      writeNcMLVariable(v, out, indent, false);
    }

    indent.decr();

    out.print(indent);
    out.print("</structure>\n");
  }

  static private void writeNcMLVariable( Variable v, PrintWriter out, Indent indent, boolean showCoords) throws IOException {
      out.print(indent);
      out.print("<variable name='"+StringUtil.quoteXmlAttribute(v.getShortName())+"' type='"+ v.getDataType()+"'");

      // any dimensions (scalers must skip this attribute) ?
      if (v.getRank() > 0) {
        writeNcMLDimension( v, out);
      }

      indent.incr();

      boolean closed = false;

      // any attributes ?
      java.util.List<Attribute> atts = v.getAttributes();
      if (atts.size() > 0) {
        out.print(" >\n");
        closed = true;
        for (Attribute att : atts) {
          writeNcMLAtt(att, out, indent);
        }
      }

      // print data ?
      if ((showCoords && v.isCoordinateVariable())) {
        if (!closed) {
           out.print(" >\n");
           closed = true;
        }
        writeNcMLValues(v, out, indent);
      }

      indent.decr();

      // close variable element
      if (!closed)
        out.print(" />\n");
      else {
        out.print(indent);
        out.print("</variable>\n");
      }

  }

  // LOOK anon dimensions
  static private void writeNcMLDimension( Variable v, PrintWriter out) {
    out.print(" shape='");
    java.util.List<Dimension> dims = v.getDimensions();
    for (int j = 0; j < dims.size(); j++) {
      Dimension dim = dims.get(j);
      if (j != 0)
        out.print(" ");
      if (dim.isShared())
        out.print(StringUtil.quoteXmlAttribute(dim.getName()));
      else
        out.print(dim.getLength());
    }
    out.print("'");
  }

  @SuppressWarnings({"ObjectToString"})
  static private void writeNcMLAtt(Attribute att, PrintWriter out, Indent indent) {
    out.print(indent);
    out.print("<attribute name='"+StringUtil.quoteXmlAttribute(att.getName())+"' value='");
    if (att.isString()) {
      for (int i=0; i<att.getLength(); i++) {
        if (i > 0) out.print("\\, "); // ??
        out.print( StringUtil.quoteXmlAttribute(att.getStringValue(i)));
      }
    } else {
     for (int i=0; i<att.getLength(); i++) {
        if (i > 0) out.print(" ");
        out.print(att.getNumericValue(i) + " ");
     }
     out.print("' type='"+att.getDataType());
    }
    out.print("' />\n");
  }

  static private int totalWidth = 80;
  static private void writeNcMLValues(Variable v, PrintWriter out, Indent indent) throws IOException {
    Array data = v.read();
    int width = formatValues(indent+"<values>", out, 0, indent);

    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext())
      width = formatValues(ii.next()+" ", out, width, indent);
    formatValues("</values>\n", out, width, indent);
  }

  static private int formatValues(String s, PrintWriter out, int width, Indent indent) {
    int len = s.length();
    if (len + width > totalWidth) {
      out.print("\n");
      out.print(indent);
      width = indent.toString().length();
    }
    out.print(s);
    width += len;
    return width;
  }

  private static char[] org = { '\b', '\f', '\n', '\r', '\t', '\\', '\'', '\"' };
  private static String[] replace = {"\\b", "\\f", "\\n", "\\r", "\\t", "\\\\", "\\\'", "\\\""};

  /**
   * Replace special characters '\t', '\n', '\f', '\r'.
   * @param s string to quote
   * @return equivilent string replacing special chars
   */
  static public String encodeString(String s) {
    return StringUtil.replace(s, org, replace);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  /**
     Main program.
    <p><strong>ucar.nc2.NCdump filename [-cdl | -ncml] [-c | -vall] [-v varName1;varName2;..] [-v varName(0:1,:,12)] </strong>
     <p>where: <ul>
   * <li> filename : path of any CDM readable file
   * <li> cdl or ncml: output format is CDL or NcML
   * <li> -vall : dump all variable data
   * <li> -c : dump coordinate variable data
   * <li> -v varName1;varName2; : dump specified variable(s)
   * <li> -v varName(0:1,:,12) : dump specified variable section
   * </ul>
   * Default is to dump the header info only.
   * @param args arguments
   */
  public static void main( String[] args) {

    if (args.length == 0) {
      System.out.println(usage);
      return;
    }

    StringBuilder sbuff = new StringBuilder();
    for (String arg : args) {
      sbuff.append(arg);
      sbuff.append(" ");
    }

    try {
      Writer writer = new BufferedWriter( new OutputStreamWriter(System.out, Charset.forName("UTF-8")));
      NCdumpW.print(sbuff.toString(),writer, null);

    } catch (java.io.IOException ioe) {
      ioe.printStackTrace();
    }
  }
}