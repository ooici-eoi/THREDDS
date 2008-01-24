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
package ucar.ma2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.ArrayList;

/**
 * Concrete implementation of ArrayStructure, data storage is in a ByteBuffer, which is converted to member data on the fly.
 * In order to use this, the records must have the same size, and the member offset must be the same for each record.
 * Use StructureMembers.setStructureSize() to set the record size.
 * Use StructureMembers.Member.setDataParam() to set the offset of the member from the start of each record.
 * The member data will then be located in the BB at offset = recnum * getStructureSize() + member.getDataParam().
 * This defers object creation for efficiency. Use getArray<type>() and getScalar<type>() data accessors if possible.
 * <pre>
     Structure pdata = (Structure) ncfile.findVariable( name);
     StructureMembers members = pdata.makeStructureMembers();
     members.findMember("value").setDataParam(0); // these are the offsets into the record
     members.findMember("x_start").setDataParam(2);
     members.findMember("y_start").setDataParam(4);
     members.findMember("direction").setDataParam(6);
     members.findMember("speed").setDataParam(8);
     int recsize = pos[1] - pos[0]; // each record  must be all the same size
     members.setStructureSize( recsize);
     ArrayStructureBB asbb = new ArrayStructureBB( members, new int[] { size}, bos, pos[0]);
 * </pre>
 * String members must store the Strings in the stringHeap. An integer index into the heap is used in the ByteBuffer.
 * @author caron
 * @see Array
 */
public class ArrayStructureBB extends ArrayStructure {
  protected ByteBuffer bbuffer;
  protected int bb_offset = 0;

  /**
   * Create a new Array of type StructureData and the given members and shape.
   * Generally, you extract the byte array and fill it: <pre>
     byte [] result = (byte []) structureArray.getStorage(); </pre>
   *
   * @param members a description of the structure members
   * @param shape   the shape of the Array.
   */
  public ArrayStructureBB(StructureMembers members, int[] shape) {
    super(members, shape);
    this.bbuffer = ByteBuffer.allocate(nelems * getStructureSize());
    bbuffer.order(ByteOrder.BIG_ENDIAN);
  }

  /**
   * Construct an ArrayStructureBB with the given ByteBuffer.
   *
   * @param members the list of structure members.
   * @param shape   the shape of the structure array
   * @param bbuffer the data is stored in this ByteBuffer. bbuffer.order must already be set.
   * @param offset  offset from the start of the ByteBufffer to the first record.
   */
  public ArrayStructureBB(StructureMembers members, int[] shape, ByteBuffer bbuffer, int offset) {
    super(members, shape);
    this.bbuffer = bbuffer;
    this.bb_offset = offset;
  }

  protected StructureData makeStructureData(ArrayStructure as, int index) {
    return new StructureDataA(as, index);
  }

  /**
   * Return backing storage as a ByteBuffer
   * @return backing storage as a ByteBuffer
   */
  public ByteBuffer getByteBuffer() {
    return bbuffer;
  }

  public double getScalarDouble(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.DOUBLE) throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be double");
    if (m.getDataArray() != null) return super.getScalarDouble(recnum, m);

    int offset = calcOffsetSetOrder(recnum, m);
    return bbuffer.getDouble(offset);
  }

  public double[] getJavaArrayDouble(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.DOUBLE) throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be double");
    if (m.getDataArray() != null) return super.getJavaArrayDouble(recnum, m);

    int offset = calcOffsetSetOrder(recnum, m);
    int count = m.getSize();
    double[] pa = new double[count];
    for (int i = 0; i < count; i++)
      pa[i] = bbuffer.getDouble(offset + i * 8);
    return pa;
  }

  protected void copyDoubles(int recnum, StructureMembers.Member m, IndexIterator result) {
    int offset = calcOffsetSetOrder(recnum, m);
    int count = m.getSize();
    for (int i = 0; i < count; i++)
      result.setDoubleNext( bbuffer.getDouble(offset + i * 8));
  }

  public float getScalarFloat(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.FLOAT) throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be float");
    if (m.getDataArray() != null) return super.getScalarFloat(recnum, m);

    int offset = calcOffsetSetOrder(recnum, m);
    return bbuffer.getFloat(offset);
  }

  public float[] getJavaArrayFloat(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.FLOAT) throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be float");
    if (m.getDataArray() != null) return super.getJavaArrayFloat(recnum, m);

    int offset = calcOffsetSetOrder(recnum, m);
    int count = m.getSize();
    float[] pa = new float[count];
    for (int i = 0; i < count; i++)
      pa[i] = bbuffer.getFloat(offset + i * 4);
    return pa;
  }

  protected void copyFloats(int recnum, StructureMembers.Member m, IndexIterator result) {
    int offset = calcOffsetSetOrder(recnum, m);
    int count = m.getSize();
    for (int i = 0; i < count; i++)
      result.setFloatNext( bbuffer.getFloat(offset + i * 4));
  }

  public byte getScalarByte(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.BYTE) throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be byte");
    if (m.getDataArray() != null) return super.getScalarByte(recnum, m);

    int offset = calcOffsetSetOrder(recnum, m);
    return bbuffer.get(offset);
  }

  public byte[] getJavaArrayByte(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.BYTE) throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be byte");
    if (m.getDataArray() != null) return super.getJavaArrayByte(recnum, m);

    int offset = calcOffsetSetOrder(recnum, m);
    int count = m.getSize();
    byte[] pa = new byte[count];
    for (int i = 0; i < count; i++)
      pa[i] = bbuffer.get(offset + i);
    return pa;
  }

  protected void copyBytes(int recnum, StructureMembers.Member m, IndexIterator result) {
    int offset = calcOffsetSetOrder(recnum, m);
    int count = m.getSize();
    for (int i = 0; i < count; i++)
      result.setByteNext( bbuffer.get(offset + i));
  }

  public short getScalarShort(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.SHORT) throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be short");
    if (m.getDataArray() != null) return super.getScalarShort(recnum, m);

    int offset = calcOffsetSetOrder(recnum, m);
    return bbuffer.getShort(offset);
  }

  public short[] getJavaArrayShort(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.SHORT) throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be short");
    if (m.getDataArray() != null) return super.getJavaArrayShort(recnum, m);

    int offset = calcOffsetSetOrder(recnum, m);
    int count = m.getSize();
    short[] pa = new short[count];
    for (int i = 0; i < count; i++)
      pa[i] = bbuffer.getShort(offset + i * 2);
    return pa;
  }

  protected void copyShorts(int recnum, StructureMembers.Member m, IndexIterator result) {
    int offset = calcOffsetSetOrder(recnum, m);
    int count = m.getSize();
    for (int i = 0; i < count; i++)
      result.setShortNext(  bbuffer.getShort(offset + i * 2));
  }

  public int getScalarInt(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.INT) throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be int");
    if (m.getDataArray() != null) return super.getScalarInt(recnum, m);

    int offset = calcOffsetSetOrder(recnum, m);
    return bbuffer.getInt(offset);
  }

  public int[] getJavaArrayInt(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.INT) throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be int");
    if (m.getDataArray() != null) return super.getJavaArrayInt(recnum, m);

    int offset = calcOffsetSetOrder(recnum, m);
    int count = m.getSize();
    int[] pa = new int[count];
    for (int i = 0; i < count; i++)
      pa[i] = bbuffer.getInt(offset + i * 4);
    return pa;
  }

  protected void copyInts(int recnum, StructureMembers.Member m, IndexIterator result) {
    int offset = calcOffsetSetOrder(recnum, m);
    int count = m.getSize();
    for (int i = 0; i < count; i++)
      result.setIntNext(  bbuffer.getInt(offset + i * 4));
  }

  public long getScalarLong(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.LONG) throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be long");
    if (m.getDataArray() != null) return super.getScalarLong(recnum, m);

    int offset = calcOffsetSetOrder(recnum, m);
    return bbuffer.getLong(offset);
  }

  public long[] getJavaArrayLong(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.LONG) throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be long");
    if (m.getDataArray() != null) return super.getJavaArrayLong(recnum, m);

    int offset = calcOffsetSetOrder(recnum, m);
    int count = m.getSize();
    long[] pa = new long[count];
    for (int i = 0; i < count; i++)
      pa[i] = bbuffer.getLong(offset + i * 8);
    return pa;
  }

  protected void copyLongs(int recnum, StructureMembers.Member m, IndexIterator result) {
    int offset = calcOffsetSetOrder(recnum, m);
    int count = m.getSize();
    for (int i = 0; i < count; i++)
      result.setLongNext(  bbuffer.getLong(offset + i * 8));
  }

  public char getScalarChar(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.CHAR) throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be char");
    if (m.getDataArray() != null) return super.getScalarChar(recnum, m);

    int offset = calcOffsetSetOrder(recnum, m);
    return (char) bbuffer.get(offset);
  }

  public char[] getJavaArrayChar(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.CHAR) throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be char");
    if (m.getDataArray() != null) return super.getJavaArrayChar(recnum, m);

    int offset = calcOffsetSetOrder(recnum, m);
    int count = m.getSize();
    char[] pa = new char[count];
    for (int i = 0; i < count; i++)
      pa[i] = (char) bbuffer.get(offset + i);
    return pa;
  }

  protected void copyChars(int recnum, StructureMembers.Member m, IndexIterator result) {
    int offset = calcOffsetSetOrder(recnum, m);
    int count = m.getSize();
    for (int i = 0; i < count; i++)
      result.setCharNext(  (char) bbuffer.get(offset + i));
  }

  public String getScalarString(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getScalarString(recnum, m);

    if (m.getDataType() == DataType.STRING) {
      int offset = calcOffsetSetOrder(recnum, m);
      int index = bbuffer.getInt(offset);
      return stringHeap.get(index);
    }

    if (m.getDataType() == DataType.CHAR) {
      int offset = calcOffsetSetOrder(recnum, m);
      int count = m.getSize();
      byte[] pa = new byte[count];
      int i;
      for (i = 0; i < count; i++) {
        pa[i] = bbuffer.get(offset + i);
        if (0 == pa[i]) break;
      }
      return new String(pa, 0, i);
    }

    throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be String or char");
  }

  public String[] getJavaArrayString(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getJavaArrayString(recnum, m);

    if (m.getDataType() == DataType.STRING) {
      int n = m.getSize();
      int offset = calcOffsetSetOrder(recnum, m);
      String[] result = new String[n];
      for (int i = 0; i < n; i++) {
        int index = bbuffer.getInt(offset + i*4);
        result[i] = stringHeap.get(index);
      }
      return result;
    }

    if (m.getDataType() == DataType.CHAR) {
      int[] shape = m.getShape();
      int rank = shape.length;
      if (rank < 2) {
        String[] result = new String[1];
        result[0] = getScalarString(recnum, m);
        return result;
      }

      int strlen = shape[rank - 1];
      int n = m.getSize() / strlen;
      int offset = calcOffsetSetOrder(recnum, m);
      String[] result = new String[n];
      for (int i = 0; i < n; i++) {
        byte[] bytes = new byte[strlen];
        for (int j = 0; j < bytes.length; j++)
          bytes[j] = bbuffer.get(offset + i * strlen + j);
        result[i] = new String(bytes);
      }
      return result;
    }

    throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be char");
  }

  protected void copyStrings(int recnum, StructureMembers.Member m, IndexIterator result) {
    int offset = calcOffsetSetOrder(recnum, m);
    int count = m.getSize();
    for (int i = 0; i < count; i++) {
      int index = bbuffer.getInt(offset + i*4);
      result.setObjectNext(  stringHeap.get(index));
    }
  }

  // LOOK - has not been tested !!!
  public StructureData getScalarStructure(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.STRUCTURE) throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be Structure");
    if (m.getDataArray() != null) return super.getScalarStructure(recnum, m);

    int offset = calcOffsetSetOrder(recnum, m);
    ArrayStructureBB subset = new ArrayStructureBB(m.getStructureMembers(), new int[]{1}, this.bbuffer, offset);

    return new StructureDataA(subset, 0);
  }

  public ArrayStructure getArrayStructure(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.STRUCTURE) throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be Structure");
    if (m.getDataArray() != null) return super.getArrayStructure(recnum, m);

    int offset = calcOffsetSetOrder(recnum, m);
    return new ArrayStructureBB(m.getStructureMembers(), m.getShape(), this.bbuffer, offset);
  }

  protected void copyStructures(int recnum, StructureMembers.Member m, IndexIterator result) {
    int count = m.getSize();
    for (int i = 0; i < count; i++)
      result.setObjectNext(  makeStructureData(this, recnum));
  }

  protected int calcOffsetSetOrder(int recnum, StructureMembers.Member m) {
    if (null != m.getDataObject())
      bbuffer.order( (ByteOrder) m.getDataObject());
    return bb_offset + recnum * getStructureSize() + m.getDataParam();
  }

  private List<String> stringHeap = new ArrayList<String>();
  public int addStringToHeap(String s) {
    stringHeap.add(s);
    return stringHeap.size() - 1;
  }

  ////////////////////////////////////////////////////////////////////////
  // debugging
  public static void main(String argv[]) {
    byte[] ba = new byte[20];
    for (int i = 0; i < ba.length; ++i)
      ba[i] = (byte) i;

    ByteBuffer bbw = ByteBuffer.wrap(ba, 5, 15);
    bbw.get(0);
    System.out.println(" bbw(0)=" + bbw.get(0) + " i would expect = 5");

    bbw.position(5);
    System.out.println(" bbw(0)=" + bbw.get(0) + " i would expect = 4");
  }

}