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
package org.kie.test.util.logging;

import java.io.OutputStream;
import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingPrintStream extends PrintStream {

    protected Logger logger;
    private StringBuilder buffer = new StringBuilder();
    protected boolean isError;
    private final PrintStream originalOutputStream;

    public LoggingPrintStream(OutputStream outputStream, boolean isError) {
        super(outputStream);
        this.originalOutputStream = (PrintStream) outputStream;
        this.isError = isError;
        String className = Thread.currentThread().getStackTrace()[2].getClassName();
        try {
            logger = LoggerFactory.getLogger(Class.forName(className));
        } catch (ClassNotFoundException e) {
            logger = LoggerFactory.getLogger(this.getClass());
        }
    }

    protected void log(String s) {
        if (isError) {
            logger.error(s);
        } else {
            logger.info(s);
        }

    }

    @Override
    public void write(int b) {
        log(String.valueOf(b));
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        if (buf == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > buf.length) || (len < 0) ||
               ((off + len) > buf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        for (int i = 0 ; i < len ; i++) {
            writeByte(buf[off + i]);
        }
        for (int i = 0 ; i < len ; i++) {
            writeByte(buf[off + i]);
        }
    }

    private void writeByte(byte b) {
        buffer.append(Byte.toString(b));
    }

    private void writeString(String s) {
        synchronized (buffer) {
            buffer.append(s);
        }
    }

    private synchronized void newLine() {
        log(buffer.toString());
        buffer.delete(0, buffer.length());

    }

    @Override
    public void print(boolean b) {
        writeString(b ? "true" : "false");
    }

    @Override
    public void print(char c) {
        writeString(String.valueOf(c));
    }

    @Override
    public void print(int i) {
        writeString(String.valueOf(i));
    }

    @Override
    public void print(long l) {
        writeString(String.valueOf(l));
    }

    @Override
    public void print(float f) {
        writeString(String.valueOf(f));
    }

    @Override
    public void print(double d) {
        writeString(String.valueOf(d));
    }

    @Override
    public void print(char[] s) {
        writeString(String.valueOf(s));
    }

    @Override
    public void print(String s) {
        writeString(s == null ? "null" : s);
    }

    @Override
    public void print(Object obj) {
        writeString(String.valueOf(obj));
    }

    @Override
    public void println() {
        newLine();
    }

    @Override
    public void println(boolean x) {
        synchronized (logger) {
            print(x);
            newLine();
        }
    }

    @Override
    public void println(char x) {
        synchronized (logger) {
            print(x);
            newLine();
        }
    }

    @Override
    public void println(int x) {
        synchronized (logger) {
            print(x);
            newLine();
        }
    }

    @Override
    public void println(long x) {
        synchronized (logger) {
            print(x);
            newLine();
        }
    }

    @Override
    public void println(float x) {
        synchronized (logger) {
            print(x);
            newLine();
        }
    }

    @Override
    public void println(double x) {
        synchronized (logger) {
            print(x);
            newLine();
        }
    }

    @Override
    public void println(char[] x) {
        synchronized (logger) {
            print(x);
            newLine();
        }
    }

    @Override
    public void println(String x) {
        synchronized (logger) {
            print(x);
            newLine();
        }
    }

    @Override
    public void println(Object x) {
        synchronized (logger) {
            print(x);
            newLine();
        }
    }

    public static void interceptSysOutSysErr() {
        System.setOut(new LoggingPrintStream(System.out, false));
        System.setErr(new LoggingPrintStream(System.err, true));
    }

    public static void restoreSysOutAndSysErr() {
        System.setOut(((LoggingPrintStream) System.out).originalOutputStream);
        System.setErr(((LoggingPrintStream) System.err).originalOutputStream);
    }
}