package thredds.catalog2.xml.parser.stax;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLEventReader;

import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.builder.ThreddsBuilderFactory;
import thredds.catalog2.builder.ThreddsMetadataBuilder;
import thredds.catalog2.builder.ThreddsBuilder;
import thredds.catalog2.simpleImpl.ThreddsMetadataImpl;
import thredds.catalog2.simpleImpl.ThreddsBuilderFactoryImpl;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TimeCoverageElementParserTest
{
  private ThreddsBuilderFactory fac;
  private ThreddsMetadataBuilder tmBldr;

  @Before
  public void createMockObjects()
  {
    this.fac = new ThreddsBuilderFactoryImpl();
    this.tmBldr = new ThreddsMetadataImpl();
  }

  @Test
  public void checkFullySpecifiedTimeCoverageElement() throws XMLStreamException, ThreddsXmlParserException
  {
    String elemName = "timeCoverage";
    //QName elemQualName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(), elemName );

    String startDate = "2009-09-17T12:00";
    String endDate = "2009-09-18T00:00";
    String duration = "12 hours";
    String resolution = "1 hours";
    StringBuilder sb = new StringBuilder()
            .append( "  <start>").append( startDate).append( "</start>\n")
            .append( "  <end>").append( endDate).append( "</end>\n")
            .append( "  <duration>").append( duration).append( "</duration>\n")
            .append( "  <resolution>").append( resolution).append( "</resolution>\n");

    String xml = StaxParserUtils.wrapContentXmlInXmlDocRootElement( elemName, null, sb.toString() );


    assertDateTypeXmlAsExpected( startDate, endDate, duration, resolution, xml );
  }

  @Test
  public void checkFullySpecifiedNoResolutionTimeCoverageElement() throws XMLStreamException, ThreddsXmlParserException
  {
    String elemName = "timeCoverage";
    //QName elemQualName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(), elemName );

    String startDate = "2009-09-17T12:00";
    String endDate = "2009-09-18T00:00";
    String duration = "12 hours";
    StringBuilder sb = new StringBuilder()
            .append( "  <start>").append( startDate).append( "</start>\n")
            .append( "  <end>").append( endDate).append( "</end>\n")
            .append( "  <duration>").append( duration).append( "</duration>\n");

    String xml = StaxParserUtils.wrapContentXmlInXmlDocRootElement( elemName, null, sb.toString() );


    assertDateTypeXmlAsExpected( startDate, endDate, duration, null, xml );
  }

  @Test
  public void checkNoDurationTimeCoverageElement() throws XMLStreamException, ThreddsXmlParserException
  {
    String elemName = "timeCoverage";
    //QName elemQualName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(), elemName );

    String startDate = "2009-09-17T12:00";
    String endDate = "2009-09-18T00:00";
    String resolution = "1 hours";
    StringBuilder sb = new StringBuilder()
            .append( "  <start>").append( startDate).append( "</start>\n")
            .append( "  <end>").append( endDate).append( "</end>\n")
            .append( "  <resolution>").append( resolution).append( "</resolution>\n");

    String xml = StaxParserUtils.wrapContentXmlInXmlDocRootElement( elemName, null, sb.toString() );


    assertDateTypeXmlAsExpected( startDate, endDate, null, resolution, xml );
  }

  @Test
  public void checkNoEndTimeCoverageElement() throws XMLStreamException, ThreddsXmlParserException
  {
    String elemName = "timeCoverage";
    //QName elemQualName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(), elemName );

    String startDate = "2009-09-17T12:00";
    String duration = "12 hours";
    String resolution = "1 hours";
    StringBuilder sb = new StringBuilder()
            .append( "  <start>").append( startDate).append( "</start>\n")
            .append( "  <duration>").append( duration).append( "</duration>\n")
            .append( "  <resolution>").append( resolution).append( "</resolution>\n");

    String xml = StaxParserUtils.wrapContentXmlInXmlDocRootElement( elemName, null, sb.toString() );


    assertDateTypeXmlAsExpected( startDate, null, duration, resolution, xml );
  }

  @Test
  public void checkNoStartSpecifiedTimeCoverageElement() throws XMLStreamException, ThreddsXmlParserException
  {
    String elemName = "timeCoverage";
    //QName elemQualName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(), elemName );

    String endDate = "present";
    String duration = "12 days";
    String resolution = "1 hours";
    StringBuilder sb = new StringBuilder()
            .append( "  <end>" ).append( endDate ).append( "</end>\n" )
            .append( "  <duration>" ).append( duration ).append( "</duration>\n" )
            .append( "  <resolution>" ).append( resolution ).append( "</resolution>\n" );

    String xml = StaxParserUtils.wrapContentXmlInXmlDocRootElement( elemName, null, sb.toString() );


    assertDateTypeXmlAsExpected( null, endDate, duration, resolution, xml );
  }

  private void assertDateTypeXmlAsExpected( String startDate, String endDate,
                                            String duration, String resolution, String xml )
          throws XMLStreamException, ThreddsXmlParserException
  {
    XMLEventReader reader = StaxParserUtils.createXmlEventReaderOnXmlString( xml, "http://test.catalog2.thredds/DateTypeParserTest/someTest.xml" );
    StaxParserUtils.advanceReaderToFirstStartElement( reader );

    assertTrue( TimeCoverageElementParser.isSelfElementStatic( reader.peek() ));

    TimeCoverageElementParser parser = new TimeCoverageElementParser( reader, this.fac, this.tmBldr );

    ThreddsBuilder bldr = parser.parse();
    assertNotNull( bldr );

    assertTrue( bldr instanceof ThreddsMetadataBuilder.DateRangeBuilder );
    ThreddsMetadataBuilder.DateRangeBuilder tmBldr = (ThreddsMetadataBuilder.DateRangeBuilder) bldr;

    assertEquals( startDate, tmBldr.getStartDate());
    assertNull( tmBldr.getStartDateFormat());
    assertEquals( endDate, tmBldr.getEndDate());
    assertNull( tmBldr.getEndDateFormat() );
    assertEquals( duration, tmBldr.getDuration() );
    assertEquals( resolution, tmBldr.getResolution() );
  }

}
