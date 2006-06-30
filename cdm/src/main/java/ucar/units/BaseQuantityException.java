// $Id: BaseQuantityException.java,v 1.6 2002/01/29 17:33:19 steve Exp $
/*
 * Copyright 1997-2000 Unidata Program Center/University Corporation for
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
package ucar.units;

import java.io.Serializable;

/**
 * Provides support for base quantity exceptions.
 * @author Steven R. Emmerson
 * @version $Id: BaseQuantityException.java,v 1.6 2002/01/29 17:33:19 steve Exp $
 */
public abstract class
BaseQuantityException
    extends	UnitException
    implements	Serializable
{
    /**
     * Constructs from nothing.
     */
    public
    BaseQuantityException()
    {}

    /**
     * Constructs from a message.
     */
    public
    BaseQuantityException(String message)
    {
	super(message);
    }
}
