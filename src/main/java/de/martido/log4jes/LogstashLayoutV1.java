/*
 * Copyright 2014 Martin Dobmeier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.martido.log4jes;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;


/**
 * A log4j {@code Layout} that transforms a logging event into a logstash-compatible JSON document.
 */
public class LogstashLayoutV1 extends Layout {

  public static final FastDateFormat ISO_8601_FORMATTER = FastDateFormat.getInstance(
      "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", TimeZone.getTimeZone("UTC"));

  private static final Integer FORMAT_VERSION = Integer.valueOf(1);

  private static final String HOSTNAME = getHostName();

  private static final int NUM_FIELDS = 6;
  private Map<String, Object> fields;

  private static final int NUM_EXCEPTION_FIELDS = 3;
  private final Map<String, Object> exception = new LinkedHashMap<>(NUM_EXCEPTION_FIELDS);

  private static final int BUF_SIZE = 256;
  private static final int BUF_MAX_CAPACITY = 1024;

  /**
   * Output buffer to append to when format() is invoked.
   * <p>
   * See {@code org.apache.log4j.PatternLayout}
   */
  private StringBuilder buf = new StringBuilder(BUF_SIZE);

  public static final Pattern TAB_PATTERN = Pattern.compile("\t");

  @Override
  public void activateOptions() {
    // Do nothing.
  }

  @Override
  public String format(LoggingEvent event) {

    this.fields = new LinkedHashMap<>(NUM_FIELDS);

    /*
     * @timestamp and @version are the only required fields
     */

    this.fields.put("@timestamp", ISO_8601_FORMATTER.format(event.getTimeStamp()));
    this.fields.put("@version", FORMAT_VERSION);

    /*
     * Add additional fields
     */

    this.fields.put("host", HOSTNAME);
    this.fields.put("level", event.getLevel().toString());
    this.fields.put("message", event.getRenderedMessage());
    this.fields.put("logger", event.getLoggerName());

    this.addLocationInformation(event);
    this.addThrowableInformation(event);

    if (this.buf.capacity() > BUF_MAX_CAPACITY) {
      this.buf = new StringBuilder(BUF_SIZE);
    } else {
      this.buf.setLength(0);
    }

    this.toJson(this.buf, this.fields);
    this.buf.append("\n");
    return this.buf.toString();
  }

  @Override
  public boolean ignoresThrowable() {
    return false;
  }

  private void addLocationInformation(LoggingEvent event) {

    LocationInfo locationInfo = event.getLocationInformation();
    if (locationInfo.getFileName() != null) {
      this.fields.put("file", locationInfo.getFileName());
    }
    this.fields.put("class", locationInfo.getClassName());
    this.fields.put("method", locationInfo.getMethodName());
    if (locationInfo.getLineNumber() != null) {
      this.fields.put("line", locationInfo.getLineNumber());
    }
  }

  private void addThrowableInformation(LoggingEvent event) {

    ThrowableInformation throwableInformation = event.getThrowableInformation();
    if (throwableInformation != null) {
      this.exception.put("class", throwableInformation.getThrowable().getClass().getName());
      this.exception.put("message", throwableInformation.getThrowable().getMessage());
      String[] stackTrace = this.escape(throwableInformation.getThrowableStrRep());
      this.exception.put("stackTrace", this.join(stackTrace));
      this.fields.put("exception", this.exception);
    }
  }

  /**
   * Must properly escape tabs for use in JSON.
   */
  private String[] escape(String[] stackTrace) {
    for (int i = 1; i < stackTrace.length; i++) {
      if (stackTrace[i].startsWith("\t")) {
        stackTrace[i] = TAB_PATTERN.matcher(stackTrace[i]).replaceFirst("\\\\t");
      }
    }
    return stackTrace;
  }

  private String join(String[] arr) {

    // Compute necessary buffer capacity
    int capacity = 0;
    for (String s : arr) {
      capacity += s.length();
    }
    capacity += (arr.length - 1) * 3;

    StringBuilder buf = new StringBuilder(capacity);

    for (int i = 0; i < arr.length; i++) {
      buf.append(arr[i]);
      if (i + 1 < arr.length) {
        buf.append("\\n");
      }
    }

    return buf.toString();
  }

  @SuppressWarnings("unchecked")
  private String toJson(StringBuilder buf, Map<String, Object> map) {

    buf.append("{");
    for (final Iterator<Entry<String, Object>> iter = map.entrySet().iterator(); iter.hasNext();) {
      final Entry<String, Object> e = iter.next();
      final String key = e.getKey();
      final Object value = e.getValue();
      buf.append("\"").append(key).append("\":");
      if (value instanceof String) {
        buf.append("\"").append(value).append("\"");
      } else if (value instanceof Number) {
        buf.append(value);
      } else if (value instanceof Map) {
        this.toJson(buf, (Map<String, Object>) value);
      } else {
        buf.append("\"").append(value).append("\"");
      }
      if (iter.hasNext()) {
        buf.append(",");
      }
    }
    buf.append("}");

    return buf.toString();
  }

  private static String getHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return "unknown";
    }
  }

}
