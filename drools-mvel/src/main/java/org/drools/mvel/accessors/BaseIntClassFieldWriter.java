/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.drools.mvel.accessors;

import java.lang.reflect.Method;

import org.drools.core.base.BaseClassFieldWriter;
import org.drools.base.base.ValueType;

public abstract class BaseIntClassFieldWriter extends BaseClassFieldWriter {

    private static final long serialVersionUID = 510l;

    public BaseIntClassFieldWriter(final Class< ? > clazz,
                                   final String fieldName) {
        super( clazz,
               fieldName );
    }

    /**
     * This constructor is not supposed to be used from outside the class hirarchy
     * 
     * @param index
     * @param fieldType
     * @param valueType
     */
    protected BaseIntClassFieldWriter(final int index,
                                      final Class< ? > fieldType,
                                      final ValueType valueType) {
        super( index,
               fieldType,
               valueType );
    }

    public BaseIntClassFieldWriter() {
    }

    public void setValue(final Object bean,
                         final Object value) {
        setIntValue( bean,
                     value == null ? 0 : ((Number) value).intValue() );
    }

    public void setBooleanValue(final Object bean,
                                final boolean value) {
        throw new RuntimeException( "Conversion to int not supported from boolean" );
    }

    public void setByteValue(final Object bean,
                             final byte value) {
        setIntValue( bean,
                     value);

    }

    public void setCharValue(final Object bean,
                             final char value) {
        throw new RuntimeException( "Conversion to int not supported from char" );
    }

    public void setDoubleValue(final Object bean,
                               final double value) {
        setIntValue( bean,
                     (int) value );
    }

    public void setFloatValue(final Object bean,
                              final float value) {
        setIntValue( bean,
                     (int) value );
    }

    public abstract void setIntValue(final Object object,
                                     final int value);

    public void setLongValue(final Object bean,
                             final long value) {
        setIntValue( bean,
                     (int) value );
    }

    public void setShortValue(final Object bean,
                              final short value) {
        setIntValue( bean,
                     value);
    }

    public Method getNativeWriteMethod() {
        try {
            return this.getClass().getDeclaredMethod("setIntValue",
                                                     Object.class, int.class);
        } catch ( final Exception e ) {
            throw new RuntimeException( "This is a bug. Please report to development team: " + e.getMessage(),
                                        e );
        }
    }

}
