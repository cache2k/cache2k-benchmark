package org.cache2k.benchmark.traces;

/*
 * #%L
 * traces
 * %%
 * Copyright (C) 2013 - 2016 headissue GmbH, Munich
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.cache2k.benchmark.util.AccessTrace;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

/**
 * Caches all traces that are read into memory already. We hold the traces so that
 * we don't need to recalculate the Belady opt efficiency and other metrics
 * again and again.
 *
 * @author Jens Wilke; created: 2013-11-20
 */
class TraceCache {

  static HashMap<String, AccessTrace> name2trace = new HashMap<>();

  static AccessTrace getTraceLazy(String _fileName) {
    AccessTrace t = name2trace.get(_fileName);
    try {
      if (t == null) {
        InputStream _resourceInput = TraceCache.class.getResourceAsStream(_fileName);
        InputStream _inputForTrace;
        if (_fileName.endsWith(".bz2")) {
          _inputForTrace = new BZip2CompressorInputStream(_resourceInput);
        } else {
          _inputForTrace = new GZIPInputStream(_resourceInput);
        }
        t = new AccessTrace(_inputForTrace);
        name2trace.put(_fileName, t);
      }
    } catch (Exception e) {
      throw new RuntimeException("Cannot load trace " + _fileName, e);
    }
    return t;
  }

  static AccessTrace getTraceLazy(String key, Provider p) {
    AccessTrace t = name2trace.get(key);
    try {
      if (t == null) {
        t = p.provide();
        name2trace.put(key, t);
      }
    } catch (Exception e) {
      throw new RuntimeException("Cannot load trace: " + key, e);
    }
    return t;
  }

  interface Provider {

    AccessTrace provide() throws IOException;
  }

}
