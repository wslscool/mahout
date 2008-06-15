/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.cf.taste.example.grouplens;

import org.apache.mahout.cf.taste.impl.common.FastMap;
import org.apache.mahout.cf.taste.impl.common.IOUtils;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.model.Item;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.NoSuchElementException;

public final class GroupLensDataModel extends FileDataModel {

  private final Map<String, Movie> movieMap;

  GroupLensDataModel() throws IOException {
    this(readResourceToTempFile("/org/apache/mahout/cf//taste/example/grouplens/ratings.dat"),
         readResourceToTempFile("/org/apache/mahout/cf//taste/example/grouplens/movies.dat"));
  }

  /**
   * @param ratingsFile GroupLens ratings.dat file in its native format
   * @param moviesFile GroupLens movies.dat file in its native format
   * @throws IOException if an error occurs while reading or writing files
   */
  public GroupLensDataModel(File ratingsFile, File moviesFile) throws IOException {
    super(convertGLFile(ratingsFile, true));
    File convertedMoviesFile = convertGLFile(moviesFile, false);
    BufferedReader reader = new BufferedReader(new FileReader(convertedMoviesFile));
    movieMap = new FastMap<String, Movie>(5001);
    try {
      for (String line; (line = reader.readLine()) != null;) {
        String[] tokens = line.split(",");
        String id = tokens[0];
        movieMap.put(id, new Movie(id, tokens[1], tokens[2]));
      }
    } finally {
      IOUtils.quietClose(reader);
    }
  }

  @Override
  protected Item buildItem(String id) {
    Item item = movieMap.get(id);
    if (item == null) {
      throw new NoSuchElementException();
    }
    return item;
  }

  private static File convertGLFile(File originalFile, boolean ratings) throws IOException {
    // Now translate the file; remove commas, then convert "::" delimiter to comma
    File resultFile = new File(new File(System.getProperty("java.io.tmpdir")),
                                     "taste." + (ratings ? "ratings" : "movies") + ".txt");
    if (!resultFile.exists()) {
      BufferedReader reader = null;
      PrintWriter writer = null;
      try {
        reader = new BufferedReader(new FileReader(originalFile), 32768);
        writer = new PrintWriter(new FileWriter(resultFile));
        for (String line; (line = reader.readLine()) != null;) {
          String convertedLine;
          if (ratings) {
            // toss the last column of data, which is a timestamp we don't want
            convertedLine = line.substring(0, line.lastIndexOf("::")).replace("::", ",");
          } else {
            convertedLine = line.replace(",", "").replace("::", ",");
          }
          writer.println(convertedLine);
        }
        writer.flush();
      } catch (FileNotFoundException fnfe) {
        resultFile.delete();
        throw fnfe;
      } catch (IOException ioe) {
        resultFile.delete();
        throw ioe;
      } finally {
        IOUtils.quietClose(writer);
        IOUtils.quietClose(reader);
      }
    }
    return resultFile;
  }

  private static File readResourceToTempFile(String resourceName) throws IOException {
    InputStream is = GroupLensRecommender.class.getResourceAsStream(resourceName);
    if (is == null) {
      is = new FileInputStream("src/example" + resourceName);
    }
    try {
      File tempFile = File.createTempFile("taste", null);
      tempFile.deleteOnExit();
      OutputStream os = new FileOutputStream(tempFile);
      try {
        int bytesRead;
        for (byte[] buffer = new byte[32768]; (bytesRead = is.read(buffer)) > 0;) {
          os.write(buffer, 0, bytesRead);
        }
        os.flush();
        return tempFile;
      } finally {
        IOUtils.quietClose(os);
      }
    } finally {
      IOUtils.quietClose(is);
    }
  }


  @Override
  public String toString() {
    return "GroupLensDataModel";
  }

}
