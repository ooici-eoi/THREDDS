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



package opendap.dap.Server;

import java.lang.String;

/**
 * The Something Bad Happened (SBH) Exception.
 * This gets thrown in situations where something
 * pretty bad went down and we don't have a good
 * exception type to describe the problem, or
 * we don't really know what the hell is going on.
 * <p/>
 * Yes, its the garbage dump of our exception
 * classes.
 *
 * @author ndp
 * @version $Revision: 15901 $
 */
public class SBHException extends DAP2ServerSideException {
    /**
     * Construct a <code>SBHException</code> with the specified
     * detail message.
     *
     * @param s the detail message.
     */
    public SBHException(String s) {
        super(opendap.dap.DAP2Exception.UNKNOWN_ERROR, "Ow! Something Bad Happened! All I know is: " + s);
    }


    /**
     * Construct a <code>SBHException</code> with the specified
     * message and OPeNDAP error code see (<code>DAP2Exception</code>).
     *
     * @param err the OPeNDAP error code.
     * @param s   the detail message.
     */
    public SBHException(int err, String s) {
        super(err, s);
    }
}


