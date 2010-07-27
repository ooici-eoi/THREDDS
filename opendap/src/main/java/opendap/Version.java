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


package opendap;

import org.jdom.Element;
import org.jdom.Text;


/**
 * Holds the version number for the java implmentation of the OPeNDAP
 * DAP2 implmentation.
 */
public final class Version {

    private final static String version = "0.0.7";

    public  static String getVersionString() {
        return (version);
    }

    public  static Element getVersionElement() {


        Element lib = new Element("lib");
        Element name = new Element("name");
        Element ver = new Element("version");

        name.addContent(new Text("java-opendap"));
        lib.addContent(name);

        ver.addContent(new Text(version));
        lib.addContent(ver);


        return (lib);

    }


}
