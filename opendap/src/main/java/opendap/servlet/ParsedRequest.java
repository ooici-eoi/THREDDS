/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2007 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////


package opendap.servlet;


/**
 * This holds the parsed information from one particular user request.
 * We neeed this to maintain thread-safety.
 * This Object is immutable, except for the "user object".
 *
 * @author jcaron
 */

public class ParsedRequest {
    private String dataSet;
    private String requestSuffix;
    private String CE;
    private boolean acceptsCompressed;
    private Object obj = null;

    public ParsedRequest(String dataSet, String requestSuffix, String CE, boolean acceptsCompressed) {
//    this.dataSet = (dataSet == null) ? "" : dataSet;
//    this.requestSuffix = (requestSuffix == null) ? "" : requestSuffix;
        this.dataSet = dataSet;
        this.requestSuffix = requestSuffix;
        this.CE = CE;
        this.acceptsCompressed = acceptsCompressed;
    }

    public String getDataSet() {
        return dataSet;
    }

    public String getRequestSuffix() {
        return requestSuffix;
    }

    public String getConstraintExpression() {
        return CE;
    }

    public boolean getAcceptsCompressed() {
        return acceptsCompressed;
    }

    // for debugging, extra state, etc
    public Object getUserObject() {
        return obj;
    }

    public void setUserObject(Object userObj) {
        this.obj = userObj;
    }

    public String toString() {
        return " dataset: " + dataSet + " suffix: " + requestSuffix + " CE: '" + CE + "' compressOK: " + acceptsCompressed;
    }


}


