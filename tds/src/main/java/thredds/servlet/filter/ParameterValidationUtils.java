package thredds.servlet.filter;

import thredds.servlet.ServletUtil;
import thredds.util.StringValidateEncodeUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Utility class for validating user input to web apps.
 *
 * All methods validate only if target (parameter e.g.) is available. If the
 * target is not available it is considered valid (unless explicitly documented
 * otherwise).
 *
 * @author edavis
 * @since 3.16.47
 * @see StringValidateEncodeUtils
 */
public class ParameterValidationUtils
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( ParameterValidationUtils.class );

  private ParameterValidationUtils() {}

  public static boolean validateParameterAsSingleValueSingleLineString( HttpServletRequest request,
                                                                        HttpServletResponse response,
                                                                        String parameterName)
          throws IOException, ServletException
  {
    String[] parameterValues = request.getParameterValues( parameterName );
    if ( parameterValues != null )
    {
      validParameterAsSingleValue( response, parameterName, parameterValues );
      if ( !StringValidateEncodeUtils.validSingleLineString( parameterValues[0] ) )
      {
        String msg = "Invalid parameter [" + parameterName + "] value [" + StringValidateEncodeUtils.encodeLogMessages( parameterValues[0] ) + "].";
        log.error( "validateParameterAsSingleValueSingleLineString(): " + msg );
        response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg );
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, msg.length() );
        return false;
      }
    }
    return true;
  }

  /**
   * blah, blah, ...
   *
   * <p>
   * <strong>Note:</strong>
   * Currently also rejecting parameter values that contain any less than ("<"),
   * greater than (">"), or backslash ("\") characters. [May loosen this
   * restriction later.]
   *
   * @param request the servlet request
   * @param response the servlet response
   * @param parameterName the name of the parameter to check
   * @return true if the parameter has a single value and is a valid path string
   * @throws IOException if there is a problem writing the response.
   * @throws ServletException if a problem is encountered.
   */
  public static boolean validateParameterAsSingleValuePathString( HttpServletRequest request,
                                                                  HttpServletResponse response,
                                                                  String parameterName)
          throws IOException, ServletException
  {
    String[] parameterValues = request.getParameterValues( parameterName );
    if ( parameterValues != null )
    {
      validParameterAsSingleValue( response, parameterName, parameterValues );
      if ( ! StringValidateEncodeUtils.validPath( parameterValues[0] )
           || StringValidateEncodeUtils.containsAngleBracketCharacters( parameterValues[0] )
           || StringValidateEncodeUtils.containsBackslashCharacters( parameterValues[0] ) )
      {
        String msg = "Invalid parameter [" + parameterName + "] value [" + StringValidateEncodeUtils.encodeLogMessages( parameterValues[0] ) + "].";
        log.error( "validateParameterAsSingleValuePathString(): " + msg );
        response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg );
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, msg.length() );
        return false;
      }
    }
    return true;
  }

  /**
   * blah, blah, ...
   *
   * <p>
   * <strong>Note:</strong>
   * Currently also rejecting parameter values that contain any less than ("<"),
   * greater than (">"), or backslash ("\") characters. [May loosen this
   * restriction later.]
   *
   * @param request the servlet request
   * @param response the servlet response
   * @param parameterName the name of the parameter to check
   * @return true if the parameter has a single value and is a valid file path string
   * @throws IOException if there is a problem writing the response.
   * @throws ServletException if a problem is encountered.
   */
  public static boolean validateParameterAsSingleValueFilePathString( HttpServletRequest request,
                                                                      HttpServletResponse response,
                                                                      String parameterName)
          throws IOException, ServletException
  {
    String[] parameterValues = request.getParameterValues( parameterName );
    if ( parameterValues != null )
    {
      validParameterAsSingleValue( response, parameterName, parameterValues );
      if ( ! StringValidateEncodeUtils.validFilePath( parameterValues[0] )
           || StringValidateEncodeUtils.containsAngleBracketCharacters( parameterValues[0] )
           || StringValidateEncodeUtils.containsBackslashCharacters( parameterValues[0] ) )
      {
        String msg = "Invalid parameter [" + parameterName + "] value [" + StringValidateEncodeUtils.encodeLogMessages( parameterValues[0] ) + "].";
        log.error( "validateParameterAsSingleValueFilePathString(): " + msg );
        response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg );
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, msg.length() );
        return false;
      }
    }
    return true;
  }

  public static boolean validateParameterAsSingleValueAlphanumericString( HttpServletRequest request,
                                                                       HttpServletResponse response,
                                                                       String parameterName)
          throws IOException, ServletException
  {
    String[] parameterValues = request.getParameterValues( parameterName );
    if ( parameterValues != null )
    {
      validParameterAsSingleValue( response, parameterName, parameterValues );
      if ( !StringValidateEncodeUtils.validAlphanumericString( parameterValues[0] ) )
      {
        String msg = "Invalid parameter [" + parameterName + "] value [" + StringValidateEncodeUtils.encodeLogMessages( parameterValues[0] ) + "].";
        log.error( "validateParameterAsSingleValueAlphanumericString(): " + msg );
        response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg );
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, msg.length() );
        return false;
      }
    }
    return true;
  }

  public static boolean validateParameterAsSingleValueAlphanumericStringConstrained( HttpServletRequest request,
                                                                                     HttpServletResponse response,
                                                                                     String parameterName,
                                                                                     String[] constrainedSet,
                                                                                     boolean ignoreCase )
          throws IOException, ServletException
  {
    String[] parameterValues = request.getParameterValues( parameterName );
    if ( parameterValues != null )
    {
      validParameterAsSingleValue( response, parameterName, parameterValues );
      if ( !StringValidateEncodeUtils.validAlphanumericStringConstrainedSet( parameterValues[0], constrainedSet, ignoreCase ) )
      {
        String msg = "Invalid parameter [" + parameterName + "] value [" + StringValidateEncodeUtils.encodeLogMessages( parameterValues[0] ) + "].";
        log.error( "validateParameterAsSingleValueAlphanumericStringConstrained(): " + msg );
        response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg );
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, msg.length() );
        return false;
      }
    }
    return true;
  }

  /**
   * blah, blah, ...
   *
   * <p>
   * <strong>Note:</strong>
   * Currently also rejecting parameter values that contain any less than ("<"),
   * greater than (">"), or backslash ("\") characters. [May loosen this
   * restriction later.]
   *
   * @param request the servlet request
   * @param response the servlet response
   * @param parameterName the name of the parameter to check
   * @return true if the parameter has a single value and is a valid URI string
   * @throws IOException if there is a problem writing the response.
   * @throws ServletException if a problem is encountered.
   */
  public static boolean validateParameterAsSingleValueUriString( HttpServletRequest request,
                                                                 HttpServletResponse response,
                                                                 String parameterName )
          throws IOException, ServletException
  {
    String[] parameterValues = request.getParameterValues( parameterName );
    if ( parameterValues != null )
    {
      validParameterAsSingleValue( response, parameterName, parameterValues );
      if ( ! StringValidateEncodeUtils.validPath( parameterValues[0] )
           || StringValidateEncodeUtils.containsAngleBracketCharacters( parameterValues[0] )
           || StringValidateEncodeUtils.containsBackslashCharacters( parameterValues[0] ) )
      {
        String msg = "Invalid parameter [" + parameterName + "] value [" + StringValidateEncodeUtils.encodeLogMessages( parameterValues[0] ) + "].";
        log.error( "validateParameterAsSingleValueUriString(): " + msg );
        response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg );
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, msg.length() );
        return false;
      }
    }
    return true;
  }

  /**
   * blah, blah, ...
   *
   * <p>
   * <strong>Note:</strong>
   * Currently also rejecting parameter values that contain any less than ("<"),
   * greater than (">"), or backslash ("\") characters. [May loosen this
   * restriction later.]
   *
   * @param request the servlet request
   * @param response the servlet response
   * @param parameterName the name of the parameter to check
   * @return true if the parameter has a single value and is a valid ID string
   * @throws IOException if there is a problem writing the response.
   * @throws ServletException if a problem is encountered.
   */
  public static boolean validateParameterAsSingleValueIdString( HttpServletRequest request,
                                                       HttpServletResponse response,
                                                       String parameterName )
          throws IOException, ServletException
  {
    String[] parameterValues = request.getParameterValues( parameterName );
    if ( parameterValues != null )
    {
      validParameterAsSingleValue( response, parameterName, parameterValues );
      if ( !StringValidateEncodeUtils.validIdString( parameterValues[0] )
           || StringValidateEncodeUtils.containsAngleBracketCharacters( parameterValues[0] )
           || StringValidateEncodeUtils.containsBackslashCharacters( parameterValues[0] ) )
      {
        String msg = "Invalid parameter [" + parameterName + "] value [" + StringValidateEncodeUtils.encodeLogMessages( parameterValues[0] ) + "].";
        log.error( "validateParameterAsSingleValueIdString(): " + msg );
        response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg );
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, msg.length() );
        return false;
      }
    }
    return true;
  }

  public static boolean validateParameterAsSingleValueBooleanString( HttpServletRequest request,
                                                                  HttpServletResponse response,
                                                                  String parameterName )
          throws IOException, ServletException
  {
    String[] parameterValues = request.getParameterValues( parameterName );
    if ( parameterValues != null )
    {
      validParameterAsSingleValue( response, parameterName, parameterValues );
      if ( !StringValidateEncodeUtils.validBooleanString( parameterValues[0] ) )
      {
        String msg = "Invalid parameter [" + parameterName + "] value [" + StringValidateEncodeUtils.encodeLogMessages( parameterValues[0] ) + "].";
        log.error( "validateParameterAsSingleValueBooleanString(): " + msg );
        response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg );
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, msg.length() );
        return false;
      }
    }
    return true;
  }

  public static boolean validateParameterAsSingleValueDecimalNumber( HttpServletRequest request,
                                                            HttpServletResponse response,
                                                            String parameterName )
          throws IOException, ServletException
  {
    String[] parameterValues = request.getParameterValues( parameterName );
    if ( parameterValues != null )
    {
      validParameterAsSingleValue( response, parameterName, parameterValues );
      if ( !StringValidateEncodeUtils.validDecimalNumber( parameterValues[0] ) )
      {
        String msg = "Invalid parameter [" + parameterName + "] value [" + StringValidateEncodeUtils.encodeLogMessages( parameterValues[0] ) + "].";
        log.error( "validateParameterAsSingleValueDecimalNumber(): " + msg );
        response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg );
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, msg.length() );
        return false;
      }
    }
    return true;
  }

  private static boolean validParameterAsSingleValue( HttpServletResponse response,
                                                      String parameterName,
                                                      String[] parameterValues )
          throws IOException, ServletException
  {
    if ( parameterValues != null )
    {
      if ( parameterValues.length > 1 )
      {
        String msg = "Multi-valued parameter [" + parameterName + "].";
        log.error( "validParameterAsSingleValue(): " + msg );
        response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg );
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, msg.length() );
        return false;
      }
    }
    return true;
  }
}
