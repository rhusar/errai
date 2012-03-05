/*
 * Copyright 2011 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.common.rebind;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Mike Brock
 */
public class ClassListReader {
  public static Set<String> getClassSetFromFile(File file) {
    InputStream inputStream = null;
    try {
      inputStream = new BufferedInputStream(new FileInputStream(file));

      byte[] buf = new byte[1024];
      int read;

      Set<String> classSet = new HashSet<String>();
      StringBuilder strBuf = new StringBuilder(64);

      while ((read = inputStream.read(buf)) != -1) {
        for (int i = 0; i < read; i++) {
          switch (buf[i]) {
            case '\n':
              classSet.add(strBuf.toString().trim());
              strBuf.delete(0, strBuf.length());
              break;
            default:
              strBuf.append((char) buf[i]);
              break;
          }
        }
      }

      if (strBuf.length() > 0) {
        classSet.add(strBuf.toString().trim());
      }

      return classSet;

    }
    catch (IOException e) {
      throw new RuntimeException("could not load file", e);
    }
    finally {
      try {
        if (inputStream != null) inputStream.close();
      }
      catch (IOException e) {
        throw new RuntimeException("error closing file", e);
      }
    }
  }
}
