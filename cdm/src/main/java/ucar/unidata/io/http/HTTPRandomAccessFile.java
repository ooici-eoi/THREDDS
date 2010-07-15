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

/*
 * HTTPRandomAccessFile.java.
 * @author John Caron, based on work by Donald Denbo
 */

package ucar.unidata.io.http;


import org.apache.http.Header;
import opendap.dap.HttpWrap;
import opendap.dap.HttpWrapException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.WritableByteChannel;
import java.nio.ByteBuffer;

/**
 * Gives access to files over HTTP, using jakarta commons HttpClient library.
 * This version uses a single instance of HttpClient, following performance guidelines at
 * http://jakarta.apache.org/commons/httpclient/performance.html
 * Plus other improvements.
 *
 * @author John Caron
 */

public class HTTPRandomAccessFile extends ucar.unidata.io.RandomAccessFile {
  static public int defaultHTTPBufferSize = 20000;

  private HttpWrap _client = null;

  /**
   * Set the HttpClient object - a single instance is used.
   * xx@param client the HttpClient object
   */
/*
  static public void setHttpClient(HttpWrap client) {
    _client = client;
  }
*/

  /**
   * Get the AbstractHttpClient object - a single instance is used.
   *
   * @return client the AbstractHttpClient object
   */
  /*
  static public HttpWrap getHttpClient() {
    return _client;
  }
  */
  // default AbstractHttpClient
  private synchronized void initHttpClient() throws HttpWrapException {
    if (_client == null)
      _client = new HttpWrap();
  }

  ///////////////////////////////////////////////////////////////////////////////////

  private String url;
  private long total_length = 0;

  private boolean debug = false, debugDetails = false;

  public HTTPRandomAccessFile(String url) throws IOException {
    this(url, defaultHTTPBufferSize);
    location = url;
  }

  public HTTPRandomAccessFile(String url, int bufferSize) throws IOException {
    super(bufferSize);
    file = null;
    this.url = url;
    location = url;

    initHttpClient();

    boolean needtest = true;

    try {
      doConnect(_client, url);

      Header head = _client.getHeader("Accept-Ranges");
      if (head == null) {
        needtest = true; // header is optional - need more testing

      } else if (head.getValue().equalsIgnoreCase("bytes")) {
        needtest = false;

      } else if (head.getValue().equalsIgnoreCase("none")) {
        throw new IOException("Server does not support byte Ranges");
      }

      head = _client.getHeader("Content-Length");
      if (head == null) {
        throw new IOException("Server does not support Content-Length");
      }

      try {
        total_length = Long.parseLong(head.getValue());
      } catch (NumberFormatException e) {
        throw new IOException("Server has malformed Content-Length header");
      }

    } finally {
      // if (_client != null) _client.close();
    }

    if (needtest && !rangeOk(url))
      throw new IOException("Server does not support byte Ranges");

    if (debugLeaks) openFiles.add(location);
  }

  private boolean rangeOk(String url) {
    try {
      _client.setHeader("Range", "bytes=" + 0 + "-" + 1);
      doConnect(_client, url);

      int code = _client.getStatusCode();
      if (code != 206)
        throw new IOException("Server does not support Range requests, code= " + code);

      // clear stream
      _client.consumeContent();
      return true;

    } catch (IOException e) {
      return false;

    } finally {
      // if (method != null) response.releaseConnection(); Cannot figure out how to do this with httpClient 4
    }
  }

  private void doConnect(HttpWrap client, String url) throws IOException {

    // Execute the method.
    client.setMethodGet(url);
    int statusCode = client.execute();

    if (statusCode == 404)
      throw new FileNotFoundException(url + " " + client.getStatusLine());


    if (statusCode >= 300)
      throw new IOException(url + " " + client.getStatusLine());

    if (debugDetails) {
      printHeaders("Request: GET " + client.getURI(), client.getHeaders());
      printHeaders("Response: " + statusCode, client.getHeaders());
    }
  }

  private void printHeaders(String title, Header[] heads) {
    System.out.println(title);
    for (Header head : heads) {
      System.out.print("  " + head.toString());
    }
    System.out.println();
  }

  /**
   * Read directly from file, without going through the buffer.
   * All reading goes through here or readToByteChannel;
   *
   * @param pos    start here in the file
   * @param buff   put data into this buffer
   * @param offset buffer offset
   * @param len    this number of bytes
   * @return actual number of bytes read
   * @throws IOException on io error
   */
  @Override
  protected int read_(long pos, byte[] buff, int offset, int len) throws IOException {
    long end = pos + len - 1;
    if (end >= total_length)
      end = total_length - 1;

    if (debug) System.out.println(" HTTPRandomAccessFile bytes=" + pos + "-" + end + ": ");

    try {
      _client.setMethodHeader("Range", "bytes=" + pos + "-" + end);
      _client.setMethodGet(url);
      int code = _client.execute();
      if (code != 206)
        throw new IOException("Server does not support Range requests, code= " + code);

      String s = _client.getHeader("Content-Length").getValue();
      if (s == null)
        throw new IOException("Server does not send Content-Length header");

      int readLen = Integer.parseInt(s);
      readLen = Math.min(len, readLen);

      InputStream is = _client.getContentStream();
      readLen = copy(is, buff, offset, readLen);
      return readLen;

    } finally {
      // if(_client != null) _client.close();
    }
  }

  private int copy(InputStream in, byte[] buff, int offset, int want) throws IOException {
    int done = 0;
    while (want > 0) {
      int bytesRead = in.read(buff, offset + done, want);
      if (bytesRead == -1) break;
      done += bytesRead;
      want -= bytesRead;
    }
    return done;
  }

  @Override
  public long readToByteChannel(WritableByteChannel dest, long offset, long nbytes) throws IOException {
    int n = (int) nbytes;
    byte[] buff = new byte[n];
    int done = read_(offset, buff, 0, n);
    dest.write(ByteBuffer.wrap(buff));
    return done;
  }

  // override selected RandomAccessFile public methods

  @Override
  public long length() throws IOException {
    long fileLength = total_length;
    if (fileLength < dataEnd)
      return dataEnd;
    else
      return fileLength;
  }

}

