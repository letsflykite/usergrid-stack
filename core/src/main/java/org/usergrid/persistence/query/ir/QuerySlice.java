/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.persistence.query.ir;

import static org.usergrid.utils.CompositeUtils.setEqualityFlag;

import java.nio.ByteBuffer;

import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.AbstractComposite.ComponentEquality;

import org.usergrid.utils.NumberUtils;

/**
 * Node that represents a query slice operation
 * 
 * @author tnine
 * 
 */
public class QuerySlice {

  private String propertyName;
  // Object value;
  private RangeValue start;
  private RangeValue finish;
  private ByteBuffer cursor;
  private boolean reversed;
  private int nodeId;

  /**
   * @param propertyName
   * @param nodeId
   */
  public QuerySlice(String propertyName, int nodeId) {
    this.propertyName = propertyName;
    this.nodeId = nodeId;
  }

  /**
   * Reverse this slice. Flips the reversed switch and correctly changes the
   * start and finish
   */
  public void reverse() {
    reversed = !reversed;

    RangeValue oldStart = start;

    start = finish;

    finish = oldStart;

  }

  public String getPropertyName() {
    return propertyName;
  }

  public void setPropertyName(String propertyName) {
    this.propertyName = propertyName;
  }

  public RangeValue getStart() {
    return start;
  }

  public void setStart(RangeValue start) {
    this.start = start;
  }

  public RangeValue getFinish() {
    return finish;
  }

  public void setFinish(RangeValue finish) {
    this.finish = finish;
  }

  public ByteBuffer getCursor() {
    return cursor;
  }

  public void setCursor(ByteBuffer cursor) {
    this.cursor = cursor;
  }

  /**
   * True if a cursor has been set
   * 
   * @return
   */
  public boolean hasCursor() {
    return this.cursor != null;
  }

  public boolean isReversed() {
    return reversed;
  }

  /**
   * Return true if we have a cursor and it's empty. This means that we've
   * already returned all possible values from this slice range with our
   * existing data in a previous invocation of search
   * 
   * @return
   */
  public boolean isComplete() {
    return cursor != null && cursor.remaining() == 0;
  }

  /**
   * True if this slice represents an equals operation
   * 
   * @return the equals
   */
  public boolean isEquals() {
    // note that both null is a full scan range and should not be considered
    // equal
    return start != null && finish != null && start.equals(finish);
  }

  /**
   * Get the slice range to be used during querying
   * 
   * @return An array of dynamic composites to use. Index 0 is the start, index
   *         1 is the finish. One or more could be null
   */
  public DynamicComposite[] getRange() {
    DynamicComposite startComposite = null;
    DynamicComposite finishComposite = null;

    // calc
    if (hasCursor()) {
      startComposite = DynamicComposite.fromByteBuffer(cursor);
    }

    else if (start != null) {
      startComposite = new DynamicComposite(start.getCode(), start.getValue());

      // forward scanning from a >= 100 OR //reverse scanning from MAX to >= 100
      if ((!reversed && !start.isInclusive()) || (reversed && start.isInclusive())) {
        setEqualityFlag(startComposite, ComponentEquality.GREATER_THAN_EQUAL);
      }

    }

    if (finish != null) {
      finishComposite = new DynamicComposite(finish.getCode(), finish.getValue());

      // forward scan to <= 100 OR reverse scan ININITY to > 100
      if ((!reversed && finish.isInclusive()) || reversed && !finish.isInclusive()) {
        setEqualityFlag(finishComposite, ComponentEquality.GREATER_THAN_EQUAL);
      }

    }

    return new DynamicComposite[] { startComposite, finishComposite };

  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((finish == null) ? 0 : finish.hashCode());
    result = prime * result
        + ((propertyName == null) ? 0 : propertyName.hashCode());
    result = prime * result + (reversed ? 1231 : 1237);
    result = prime * result + ((start == null) ? 0 : start.hashCode());
    result = prime * result + nodeId;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    QuerySlice other = (QuerySlice) obj;
    if (finish == null) {
      if (other.finish != null) {
        return false;
      }
    } else if (!finish.equals(other.finish)) {
      return false;
    }
    if (propertyName == null) {
      if (other.propertyName != null) {
        return false;
      }
    } else if (!propertyName.equals(other.propertyName)) {
      return false;
    }
    if (reversed != other.reversed) {
      return false;
    }
    if (start == null) {
      if (other.start != null) {
        return false;
      }
    } else if (!start.equals(other.start)) {
      return false;
    }
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "QuerySlice [propertyName=" + propertyName + ", start=" + start
        + ", finish=" + finish + ", cursor=" + cursor + ", reversed="
        + reversed + ", nodeId=" + nodeId + "]";
  }

  public static class RangeValue {
    byte code;
    Object value;
    boolean inclusive;

    public RangeValue(byte code, Object value, boolean inclusive) {
      this.code = code;
      this.value = value;
      this.inclusive = inclusive;
    }

    public byte getCode() {
      return code;
    }

    public void setCode(byte code) {
      this.code = code;
    }

    public Object getValue() {
      return value;
    }

    public void setValue(Object value) {
      this.value = value;
    }

    public boolean isInclusive() {
      return inclusive;
    }

    public void setInclusive(boolean inclusive) {
      this.inclusive = inclusive;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + code;
      result = prime * result + (inclusive ? 1231 : 1237);
      result = prime * result + ((value == null) ? 0 : value.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      RangeValue other = (RangeValue) obj;
      if (code != other.code) {
        return false;
      }
      if (inclusive != other.inclusive) {
        return false;
      }
      if (value == null) {
        if (other.value != null) {
          return false;
        }
      } else if (!value.equals(other.value)) {
        return false;
      }
      return true;
    }

    public int compareTo(RangeValue other, boolean finish) {
      if (other == null) {
        return 1;
      }
      if (code != other.code) {
        return NumberUtils.sign(code - other.code);
      }
      @SuppressWarnings({ "unchecked", "rawtypes" })
      int c = ((Comparable) value).compareTo(other.value);
      if (c != 0) {
        return c;
      }
      if (finish) {
        // for finish values, inclusive means <= which is greater than <
        if (inclusive != other.inclusive) {
          return inclusive ? 1 : -1;
        }
      } else {
        // for start values, inclusive means >= which is lest than >
        if (inclusive != other.inclusive) {
          return inclusive ? -1 : 1;
        }
      }
      return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return "RangeValue [code=" + code + ", value=" + value
          + ", inclusive=" + inclusive + "]";
    }

    public static int compare(RangeValue v1, RangeValue v2, boolean finish) {
      if (v1 == null) {
        if (v2 == null) {
          return 0;
        }
        return -1;
      }
      return v1.compareTo(v2, finish);
    }
  }

}