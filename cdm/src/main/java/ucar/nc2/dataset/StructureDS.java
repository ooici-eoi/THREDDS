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

package ucar.nc2.dataset;

import ucar.nc2.*;
import ucar.ma2.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.EnumSet;

/**
 * An "enhanced" Structure.
 *
 * @author john caron
 * @see NetcdfDataset
 */

public class StructureDS extends ucar.nc2.Structure implements VariableEnhanced {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StructureDS.class);

  private EnhancementsImpl proxy;

  protected Structure orgVar; // wrap this Variable
  private String orgName; // in case Variable wwas renamed, and we need the original name for aggregation

  /**
   * Constructor when theres no underlying variable. You better set the values too!
   *
   * @param ds              the containing NetcdfDataset.
   * @param group           the containing group; if null, use rootGroup
   * @param parentStructure parent Structure, may be null
   * @param shortName       variable shortName, must be unique within the Group
   * @param dims            list of dimension names, space delimited
   * @param units           unit string (may be null)
   * @param desc            description (may be null)
   */
  public StructureDS(NetcdfDataset ds, Group group, Structure parentStructure, String shortName,
          String dims, String units, String desc) {

    super(ds, group, parentStructure, shortName);
    setDimensions(dims);
    this.proxy = new EnhancementsImpl(this, units, desc);

    if (units != null)
      addAttribute(new Attribute("units", units));
    if (desc != null)
      addAttribute(new Attribute("long_name", desc));
  }

  /**
   * Create a StructureDS thats wraps a Structure
   *
   * @param g      parent group
   * @param orgVar original Structure
   */
  public StructureDS(Group g, ucar.nc2.Structure orgVar) { // , boolean reparent) {
    super(orgVar);
    this.group = g;
    this.orgVar = orgVar;
    this.proxy = new EnhancementsImpl(this);

    // dont share cache, iosp : all IO is delegated
    this.ncfile = null;
    this.spiObject = null;
    this.preReader = null;
    this.postReader = null;
    createNewCache();

    if (orgVar instanceof StructureDS)
      return;

    // all member variables must be wrapped, reparented
    List<Variable> newList = new ArrayList<Variable>(members.size());
    for (Variable v : members) {
      Variable newVar = (v instanceof Structure) ? new StructureDS(g, (Structure) v) : new VariableDS(g, v, false);
      newVar.setParentStructure(this);
      newList.add(newVar);
    }
    setMemberVariables(newList);
  }

  /**
   * Wrap the given Structure, making it into a StructureDS.
   * Delegate data reading to the original variable.
   * Does not share cache, iosp.
   * This is for NcML explicit mode
   *
   * @param group     the containing group; may not be null
   * @param parent    parent Structure, may be null
   * @param shortName variable shortName, must be unique within the Group
   * @param orgVar    the original Structure to wrap.
   */
  public StructureDS(Group group, Structure parent, String shortName, Structure orgVar) {
    super(null, group, parent, shortName);

    if (orgVar instanceof Structure)
      throw new IllegalArgumentException("VariableDS must not wrap a Structure; name=" + orgVar.getName());

    // dont share cache, iosp : all IO is delegated
    this.ncfile = null;
    this.spiObject = null;
    this.preReader = null;
    this.postReader = null;
    createNewCache();

    this.orgVar = orgVar;
  }

  // for section and slice
  @Override
  protected Variable copy() {
    return new StructureDS(null, this);
  }

  @Override
  public Structure select(List<Variable> members) {
    StructureDS result = new StructureDS(group, orgVar);
    result.setMemberVariables(members);
    result.isSubset = true;
    return result;
  }

  /**
   * Create a subset of the Structure consisting only of the one member variable
   *
   * @param v Variable
   * @return subsetted Structure
   */
  @Override
  public Structure select(Variable v) {
    StructureDS result = new StructureDS(group, orgVar);
    List<Variable> members = new ArrayList<Variable>(1);
    members.add(v);
    result.setMemberVariables(members);
    result.isSubset = true;
    return result;
  }

  /**
   * A StructureDS may wrap another Structure.
   *
   * @return original Structure or null
   */
  public ucar.nc2.Variable getOriginalVariable() {
    return orgVar;
  }

  /**
   * Set the Structure to wrap.
   *
   * @param orgVar original Variable, must be a Structure
   */
  public void setOriginalVariable(ucar.nc2.Variable orgVar) {
    if (!(orgVar instanceof Structure))
      throw new IllegalArgumentException("STructureDS must wrap a Structure; name=" + orgVar.getName());
    this.orgVar = (Structure) orgVar;
  }

  /**
   * When this wraps another Variable, get the original Variable's DataType.
   *
   * @return original Variable's DataType
   */
  public DataType getOriginalDataType() {
    return DataType.STRUCTURE;
  }

  /**
   * When this wraps another Variable, get the original Variable's DataType.
   *
   * @return original Variable's DataType
   */
  public String getOriginalName() {
    return orgName;
  }

  @Override
  public void setName(String newName) {
    this.orgName = shortName;
    super.setName(newName);
  }

  /**
   * Set the proxy reader.
   *
   * @param proxyReader set to this
   */
  public void setProxyReader(ProxyReader proxyReader) {
    this.postReader = proxyReader;
  }

  /**
   * Get the proxy reader, or null.
   *
   * @return return the proxy reader, if any
   */
  public ProxyReader getProxyReader() {
    return this.postReader;
  }

  // regular Variables.
  @Override
  protected Array _read() throws IOException {
    Array result;

    if (hasCachedData())
      result = super._read();
    else if (postReader != null)
      result = postReader.read(this, null);
    else if (orgVar != null)
      result = orgVar.read();
    else {
      throw new IllegalStateException("StructureDS has no way to get data");
      //Object data = smProxy.getFillValue(getDataType());
      //return Array.factoryConstant(dataType.getPrimitiveClassType(), getShape(), data);
    }

    return convert(result, null);
  }

  // section of regular Variable
  @Override
  protected Array _read(Section section) throws IOException, InvalidRangeException {
    if (section.computeSize() == getSize())
      return _read();

    Array result;
    if (hasCachedData())
      result = super._read(section);
    else if (postReader != null)
      result = postReader.read(this, section, null);
    else if (orgVar != null)
      result = orgVar.read(section);
    else {
      throw new IllegalStateException("StructureDS has no way to get data");
      //Object data = smProxy.getFillValue(getDataType());
      //return Array.factoryConstant(dataType.getPrimitiveClassType(), section.getShape(), data);
    }

    // do any needed conversions (scale/offset, enums, etc)
    return convert(result, section);
  }

  ///////////////////////////////////////

  // is conversion needed?

  private boolean convertNeeded(StructureMembers smData) {

    for (Variable v : getVariables()) {

      if (v instanceof VariableDS) {
        VariableDS vds = (VariableDS) v;
        if (vds.getNeedScaleOffsetMissing() || vds.getNeedEnumConversion())
          return true;
      }

      if (v instanceof StructureDS) {
        StructureDS nested = (StructureDS) v;
        if (nested.convertNeeded(null))
          return true;
      }

      // a variable with no data in the underlying smData
      if ((smData != null) && !varHasData(v, smData))
        return true;
    }

    return false;
  }

  // posibile things needed:
  //   1) scale/offset/enum conversion
  //   2) name, info change
  //   3) variable with cached data added to StructureDS through NcML
  private Array convert(Array data, Section section) throws IOException {
    ArrayStructure orgAS = (ArrayStructure) data;
    if (!convertNeeded(orgAS.getStructureMembers())) {
      // name, info change only
      convertMemberInfo(orgAS.getStructureMembers());
      return data;
    }

    // LOOK! converting to ArrayStructureMA
    // do any scale/offset/enum conversions
    ArrayStructure newAS = ArrayStructureMA.factoryMA(orgAS);
    for (StructureMembers.Member m : newAS.getMembers()) {
      VariableEnhanced v2 = (VariableEnhanced) findVariable(m.getName());
      if ((v2 == null) && (orgVar != null)) // these are from orgVar - may have been renamed
        v2 = findVariableFromOrgName(m.getName());
      if (v2 == null) continue;

      if (v2 instanceof VariableDS) {
        VariableDS vds = (VariableDS) v2;
        if (vds.getNeedScaleOffsetMissing()) {
          Array mdata = newAS.extractMemberArray(m);
          mdata = vds.convertScaleOffsetMissing(mdata);
          newAS.setMemberArray(m, mdata);

        } else if (vds.getNeedEnumConversion()) {
          Array mdata = newAS.extractMemberArray(m);
          //System.out.print(" convert "+vds.getName()+" array="+mdata.hashCode());
          mdata = vds.convertEnums(mdata);
          newAS.setMemberArray(m, mdata);
          //System.out.println(" to  array="+mdata.hashCode());
        }

      } else if (v2 instanceof StructureDS) {
        StructureDS innerStruct = (StructureDS) v2;
        if (innerStruct.convertNeeded(null)) {

          if (innerStruct.getDataType() == DataType.SEQUENCE) {
            ArrayObject.D1 seqArray = (ArrayObject.D1) newAS.extractMemberArray(m);
            ArrayObject.D1 newSeq = new ArrayObject.D1(ArraySequence.class, (int) seqArray.getSize());
            m.setDataArray(newSeq); // put back into member array

            // wrap each Sequence
            for (int i = 0; i < seqArray.getSize(); i++) {
              ArraySequence innerSeq = (ArraySequence) seqArray.get(i); // get old ArraySequence
              newSeq.set(i, new SequenceConverter(innerStruct, innerSeq)); // wrap in converter
            }

            // non-Sequence Structures
          } else {
            Array mdata = newAS.extractMemberArray(m);
            mdata = innerStruct.convert(mdata, null);
            newAS.setMemberArray(m, mdata);
          }

        }

        // always convert the inner StructureMembers
        innerStruct.convertMemberInfo(m.getStructureMembers());
      }
    }

    StructureMembers sm = newAS.getStructureMembers();
    convertMemberInfo(sm);

    // check for variables that have been added by NcML
    for (Variable v : getVariables()) {
      if (!varHasData(v, sm)) {
        try {
          Variable completeVar = getParentGroup().findVariable(v.getShortName()); // LOOK BAD
          Array mdata = completeVar.read(section);
          StructureMembers.Member m = sm.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), v.getShape());
          newAS.setMemberArray(m, mdata);
        } catch (InvalidRangeException e) {
          throw new IOException(e.getMessage());
        }
      }
    }

    return newAS;
  }

  /* convert original structureData to one that conforms to this Structure */
  protected StructureData convert(StructureData sdata, int recno) throws IOException {
    if (!convertNeeded(sdata.getStructureMembers())) {
      // name, info change only
      convertMemberInfo(sdata.getStructureMembers());
      return sdata;
    }

    StructureMembers smResult = new StructureMembers(sdata.getStructureMembers());
    StructureDataW result = new StructureDataW(smResult);

    for (StructureMembers.Member m : sdata.getMembers()) {
      VariableEnhanced v2 = (VariableEnhanced) findVariable(m.getName());
      if ((v2 == null) && (orgVar != null))
        v2 = findVariableFromOrgName(m.getName());
      if (v2 == null) {
        log.warn("StructureDataDS.convert Cant find member " + m.getName());
        continue;
      }

      StructureMembers.Member mResult = smResult.findMember(m.getName());

      if (v2 instanceof VariableDS) {
        VariableDS vds = (VariableDS) v2;
        Array mdata = sdata.getArray(m);

        if (vds.getNeedScaleOffsetMissing())
          mdata = vds.convertScaleOffsetMissing(mdata);
        else if (vds.getNeedEnumConversion())
          mdata = vds.convertEnums(mdata);

        result.setMemberData(mResult, mdata);
      }

      // recurse into sub-structures
      if (v2 instanceof StructureDS) {
        StructureDS innerStruct = (StructureDS) v2;
        if (innerStruct.convertNeeded(null)) {

          if (innerStruct.getDataType() == DataType.SEQUENCE) {
            Array a = sdata.getArray(m);

            if (a instanceof ArrayObject.D1) { // LOOK when does this happen vs ArraySequence?
              ArrayObject.D1 seqArray = (ArrayObject.D1) a;
              ArrayObject.D1 newSeq = new ArrayObject.D1(ArraySequence.class, (int) seqArray.getSize());
              mResult.setDataArray(newSeq); // put into result member array

              for (int i = 0; i < seqArray.getSize(); i++) {
                ArraySequence innerSeq = (ArraySequence) seqArray.get(i); // get old ArraySequence
                newSeq.set(i, new SequenceConverter(innerStruct, innerSeq)); // wrap in converter
              }

            } else {
              ArraySequence seqArray = (ArraySequence) a;
              result.setMemberData(mResult, new SequenceConverter(innerStruct, seqArray)); // wrap in converter
            }

            // non-Sequence Structures
          } else {
            Array mdata = sdata.getArray(m);
            mdata = innerStruct.convert(mdata, null);
            result.setMemberData(mResult, mdata);
          }
        }

        // always convert the inner StructureMembers
        innerStruct.convertMemberInfo(mResult.getStructureMembers());
      }
    }

    StructureMembers sm = result.getStructureMembers();
    convertMemberInfo(sm);

    // check for variables that have been added by NcML
    for (Variable v : getVariables()) {
      if (!varHasData(v, sm)) {
        try {
          Variable completeVar = getParentGroup().findVariable(v.getShortName()); // LOOK BAD
          Array mdata = completeVar.read(new Section().appendRange(recno, recno));
          StructureMembers.Member m = sm.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), v.getShape());
          result.setMemberData(m, mdata);
        } catch (InvalidRangeException e) {
          throw new IOException(e.getMessage());
        }
      }
    }

    return result;
  }

  // the wrapper StructureMembers must be converted to correspond to the wrapper Structure
  private void convertMemberInfo(StructureMembers wrapperSm) {
    for (StructureMembers.Member m : wrapperSm.getMembers()) {
      Variable v = findVariable(m.getName());
      if ((v == null) && (orgVar != null)) // may have been renamed
        v = (Variable) findVariableFromOrgName(m.getName());

      if (v == null)
        log.error("Cant find " + m.getName());
      else
        m.setVariableInfo(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType());

      // nested structures
      if (v instanceof StructureDS) {
        StructureDS innerStruct = (StructureDS) v;
        innerStruct.convertMemberInfo(m.getStructureMembers());
      }

    }
  }

  // look for the top variable that has an orgVar with the wanted orgName
  private VariableEnhanced findVariableFromOrgName(String orgName) {
    for (Variable vTop : getVariables()) {
      Variable v = vTop;
      while (v instanceof VariableEnhanced) {
        VariableEnhanced ve = (VariableEnhanced) v;
        if (ve.getOriginalName().equals(orgName)) return (VariableEnhanced) vTop;
        v = ve.getOriginalVariable();
      }
    }
    return null;
  }

  // verify that the variable has data in the data array
  private boolean varHasData(Variable v, StructureMembers sm) {
    if (sm.findMember(v.getShortName()) != null) return true;
    while (v instanceof VariableEnhanced) {
      VariableEnhanced ve = (VariableEnhanced) v;
      if (sm.findMember(ve.getOriginalName()) != null) return true;
      v = ve.getOriginalVariable();
    }
    return false;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private class SequenceConverter extends ArraySequence {
    StructureDS orgStruct;
    ArraySequence orgSeq;

    SequenceConverter(StructureDS orgStruct, ArraySequence orgSeq) {
      super(orgSeq.getStructureMembers(), orgSeq.getShape());
      this.orgStruct = orgStruct;
      this.orgSeq = orgSeq;

      // copay and convert the members
      members = new StructureMembers(orgSeq.getStructureMembers());
      orgStruct.convertMemberInfo(members);
    }

    @Override
    public StructureDataIterator getStructureDataIterator() throws java.io.IOException {
      return new StructureDataConverter(orgStruct, orgSeq.getStructureDataIterator());
    }
  }

  private class StructureDataConverter implements StructureDataIterator {
    StructureDataIterator orgIter;
    StructureDS newStruct;
    int count = 0;

    StructureDataConverter(StructureDS newStruct, StructureDataIterator orgIter) {
      this.newStruct = newStruct;
      this.orgIter = orgIter;
    }

    public boolean hasNext() throws IOException {
      return orgIter.hasNext();
    }

    public StructureData next() throws IOException {
      StructureData sdata = orgIter.next();
      return newStruct.convert(sdata, count++);
    }

    public void setBufferSize(int bytes) {
      orgIter.setBufferSize(bytes);
    }

    public StructureDataIterator reset() {
      orgIter.reset();
      return this;
    }
  }

  public StructureDataIterator getStructureIterator(int bufferSize) throws java.io.IOException {
    StructureDataIterator iter = orgVar.getStructureIterator(bufferSize);
    return new StructureDataConverter(this, iter);
  }

  ///////////////////////////////////////////////////////////

  /**
   * DO NOT USE DIRECTLY. public by accident.
   * recalc any enhancement info
   */
  public void enhance(EnumSet<NetcdfDataset.Enhance> mode) {
    for (Variable v : getVariables()) {
      VariableEnhanced ve = (VariableEnhanced) v;
      ve.enhance(mode);
    }
  }

  public void addCoordinateSystem(ucar.nc2.dataset.CoordinateSystem p0) {
    proxy.addCoordinateSystem(p0);
  }

  public void removeCoordinateSystem(ucar.nc2.dataset.CoordinateSystem p0) {
    proxy.removeCoordinateSystem(p0);
  }

  public java.util.List<CoordinateSystem> getCoordinateSystems() {
    return proxy.getCoordinateSystems();
  }

  public java.lang.String getDescription() {
    return proxy.getDescription();
  }

  public java.lang.String getUnitsString() {
    return proxy.getUnitsString();
  }

  public void setUnitsString(String units) {
    proxy.setUnitsString(units);
  }

}
