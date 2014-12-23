package com.github.sommeri.less4j.utils;

import java.io.File;
import java.net.URI;
import java.util.regex.Pattern;

/**
 * This class is here because we need to relativize paths, and Java 6 in fact has a method for this on URI,
 * however it does not deal with all cases. I do want to support Java 6 with this plugin (sadly), thus cannot
 * use Paths, which already has this properly implemented (but is Java 7).
 *
 * Based on https://raw.githubusercontent.com/SomMeri/less4j/0e530bebc9c5ea02a5eeddcf521c27c2ae40b024/src/main/java/com/github/sommeri/less4j/utils/URIUtils.java
 * Which is under the Apache 2.0 license.
 */
public class URIUtils {

  public static final String URI_FILE_SEPARATOR = "/";

  public static String relativize(File from, File to) {
    URI fromURI = from.toURI();
    URI toURI = to.toURI();

    return getRelativePath(fromURI.toString(), toURI.toString(), URIUtils.URI_FILE_SEPARATOR);
  }

  public static String relativizeSourceURIs(URI from, URI to) {
    if (to == null)
      return null;

    String toURIAsString = to.toString();

    if (from == null)
      return toURIAsString;

    String fromURIAsString = from.toString();

    return getRelativePath(fromURIAsString, toURIAsString, URIUtils.URI_FILE_SEPARATOR);
  }

  /**
   * taken from stackoverflow:
   * http://stackoverflow.com/questions/204784/how-to-construct
   * -a-relative-path-in-java-from-two-absolute-paths-or-urls
   * 
   * Get the relative path from one file to another, specifying the directory
   * separator. If one of the provided resources does not exist, it is assumed
   * to be a file unless it ends with '/' or '\'.
   * 
   * @param basePath
   *          basePath is calculated from this file
   * @param targetPath
   *          targetPath is calculated to this file
   * @param pathSeparator
   *          directory separator. The platform default is not assumed so that
   *          we can test Unix behaviour when running on Windows (for example)
   * 
   * @return
   */
  public static String getRelativePath(String basePath, String targetPath, String pathSeparator) {
    if (targetPath ==null)
      return "";
    
    if (basePath ==null)
      return targetPath;

    String[] base = basePath.split(Pattern.quote(pathSeparator));
    String[] target = targetPath.split(Pattern.quote(pathSeparator));

    // First get all the common elements. Store them as a string,
    // and also count how many of them there are.
    StringBuilder common = new StringBuilder();

    int commonIndex = 0;
    while (commonIndex < target.length && commonIndex < base.length && target[commonIndex].equals(base[commonIndex])) {
      common.append(target[commonIndex] + pathSeparator);
      commonIndex++;
    }

    if (commonIndex == 0) {
      // No single common path element. This most
      // likely indicates differing drive letters, like C: and D:.
      // These paths cannot be relativized.
      return targetPath;
      //      throw new PathResolutionException("No common path element found for '" + normalizedTargetPath + "' and '" + normalizedBasePath + "'");
    }

    // The number of directories we have to backtrack depends on whether the base is a file or a dir
    // For example, the relative path from
    //
    // /foo/bar/baz/gg/ff to /foo/bar/baz
    // 
    // ".." if ff is a file
    // "../.." if ff is a directory
    //
    // The following is a heuristic to figure out if the base refers to a file or dir. It's not perfect, because
    // the resource referred to by this path may not actually exist, but it's the best I can do
    boolean baseIsFile = true;

    File baseResource = new File(basePath);

    if (baseResource.exists()) {
      baseIsFile = baseResource.isFile();

    } else if (basePath.endsWith(pathSeparator)) {
      baseIsFile = false;
    }

    StringBuilder relative = new StringBuilder();

    if (base.length != commonIndex) {
      int numDirsUp = baseIsFile ? base.length - commonIndex - 1 : base.length - commonIndex;

      for (int i = 0; i < numDirsUp; i++) {
        relative.append(".." + pathSeparator);
      }
    }
    relative.append(safeSubstring(targetPath, common.length()));
    return relative.toString();
  }

  private static String safeSubstring(String string, int beginIndex) {
    if (beginIndex > string.length())
      return "";

    return string.substring(beginIndex);
  }

}