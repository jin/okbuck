package com.uber.okbuck.extension;

import com.google.common.collect.Sets;

import java.io.File;
import java.util.Set;

public class WrapperExtension {

  /** Custom buck repository to add as a remote to the wrapper buck installation */
  public String repo = "";

  /** List of changed files to trigger okbuck runs on */
  public Set<String> watch =
      Sets.newHashSet(
          "**/*.pro",
          "**/*.map",
          "**/*.gradle",
          "**/src/**/AndroidManifest.xml",
          "**/gradle-wrapper.properties",
          "**/lint.xml");

  /** List of added/removed directories to trigger okbuck runs on */
  public Set<String> sourceRoots =
      Sets.newHashSet("**/src/**/java", "**/src/**/res", "**/src/**/resources");

  /** List of directories to ignore when querying for changes that should trigger okbuck runs */
  public Set<String> ignoredDirs = Sets.newHashSet(".okbuck");

  public File wrapperFile;

  public String wrapperTemplate;
}
