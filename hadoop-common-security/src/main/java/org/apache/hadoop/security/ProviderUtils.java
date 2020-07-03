/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.security;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for both key and credential provider APIs.
 *
 */
public final class ProviderUtils {
  @VisibleForTesting
  public static final String NO_PASSWORD_WARN =
      "WARNING: You have accepted the use of the default provider password\n" +
      "by not configuring a password in one of the two following locations:\n";
  @VisibleForTesting
  public static final String NO_PASSWORD_ERROR =
      "ERROR: The provider cannot find a password in the expected " +
      "locations.\nPlease supply a password using one of the " +
      "following two mechanisms:\n";
  @VisibleForTesting
  public static final String NO_PASSWORD_CONT =
      "Continuing with the default provider password.\n";
  @VisibleForTesting
  public static final String NO_PASSWORD_INSTRUCTIONS_DOC =
      "Please review the documentation regarding provider passwords in\n" +
      "the keystore passwords section of the Credential Provider API\n";

  private static final Logger LOG =
      LoggerFactory.getLogger(ProviderUtils.class);

  /**
   * Hidden ctor to ensure that this utility class isn't
   * instantiated explicitly.
   */
  private ProviderUtils() {
    // hide ctor for checkstyle compliance
  }



  /**
   * The password is either found in the environment or in a file. This
   * routine implements the logic for locating the password in these
   * locations.
   *
   * @param envWithPass  The name of the environment variable that might
   *                     contain the password. Must not be null.
   * @param fileWithPass The name of a file that could contain the password.
   *                     Can be null.
   * @return The password as a char []; null if not found.
   * @throws IOException If fileWithPass is non-null and points to a
   * nonexistent file or a file that fails to open and be read properly.
   */
  public static char[] locatePassword(String envWithPass, String fileWithPass)
      throws IOException {
    char[] pass = null;
    if (System.getenv().containsKey(envWithPass)) {
      pass = System.getenv(envWithPass).toCharArray();
    }
    if (pass == null) {
      if (fileWithPass != null) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL pwdFile = cl.getResource(fileWithPass);
        if (pwdFile == null) {
          // Provided Password file does not exist
          throw new IOException("Password file does not exist");
        }
        try (InputStream is = pwdFile.openStream()) {
          pass = IOUtils.toString(is).trim().toCharArray();
        }
      }
    }
    return pass;
  }

  private static String noPasswordInstruction(String envKey, String fileKey) {
    return
        "    * In the environment variable " + envKey + "\n" +
        "    * In a file referred to by the configuration entry\n" +
        "      " + fileKey + ".\n" +
        NO_PASSWORD_INSTRUCTIONS_DOC;
  }

  public static String noPasswordWarning(String envKey, String fileKey) {
    return NO_PASSWORD_WARN + noPasswordInstruction(envKey, fileKey) +
        NO_PASSWORD_CONT;
  }

  public static String noPasswordError(String envKey, String fileKey) {
    return NO_PASSWORD_ERROR + noPasswordInstruction(envKey, fileKey);
  }
}
