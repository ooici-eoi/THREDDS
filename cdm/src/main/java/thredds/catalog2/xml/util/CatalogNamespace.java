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
package thredds.catalog2.xml.util;

import thredds.util.HttpUriResolver;
import thredds.util.HttpUriResolverFactory;

import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.xml.sax.SAXException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public enum CatalogNamespace
{
  CATALOG_1_0( "",
               "http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0",
               "/resources/thredds/schemas/InvCatalog.1.0.2.xsd",
               "http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.0.2.xsd"),
  CATALOG_0_6( "oldThredds",
               "http://www.unidata.ucar.edu/thredds",
               "/resources/thredds/schemas/InvCatalog.0.6.xsd",
               "http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.0.6.xsd"),
  XLINK( "xlink",
         "http://www.w3.org/1999/xlink",
         "/resources/thredds/schemas/xlink.xsd",
         // "/resources/schemas/xlink/1.0.0/xlinks.xsd", // OGC version
         "");

  private String standardPrefix;
  private String namespaceUri;
  private String resourceName;
  private URI resourceUri;

  CatalogNamespace( String standardPrefix, String namespaceUri, String resourceName, String resourceUri )
  {
    this.standardPrefix = standardPrefix;
    this.namespaceUri = namespaceUri;
    this.resourceName = resourceName;
    try
    {
      this.resourceUri = new URI( resourceUri);
    }
    catch ( URISyntaxException e )
    {
      throw new IllegalArgumentException( "Badly formed resource URI [" + resourceUri + "].", e);
    }
  }

  public String getStandardPrefix()
  {
    return this.standardPrefix;
  }
  
  public String getNamespaceUri()
  {
    return this.namespaceUri;
  }

  public String getResourceName()
  {
    return this.resourceName;
  }

  public URI getResourceUri()
  {
    return this.resourceUri;
  }

  public static CatalogNamespace getNamespace( String namespaceUri )
  {
    if ( namespaceUri == null ) return null;
    for ( CatalogNamespace curNs : CatalogNamespace.values() )
    {
      if ( curNs.namespaceUri.equals( namespaceUri ) )
        return curNs;
    }
    return null;
  }

  public InputStream resolveNamespace()
          throws IOException
  {
    InputStream inStream = null;
    if ( this.getResourceName() != null )
      inStream = this.getClass().getClassLoader().getResourceAsStream( this.getResourceName() );
    if ( inStream == null && this.getResourceUri() != null )
    {
      HttpUriResolver httpUriResolver = HttpUriResolverFactory.getDefaultHttpUriResolver( this.getResourceUri() );
      httpUriResolver.makeRequest();
      inStream = httpUriResolver.getResponseBodyAsInputStream();
    }

    return inStream;
  }

  public Schema resolveNamespaceAsSchema()
          throws IOException, SAXException
  {
    SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
    StreamSource source = new StreamSource( this.resolveNamespace() );
    source.setSystemId( this.getResourceUri().toString() );
    return schemaFactory.newSchema( source );
  }
}
