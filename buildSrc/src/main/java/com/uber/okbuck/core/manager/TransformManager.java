package com.uber.okbuck.core.manager;

import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.DependencyUtils;
import com.uber.okbuck.core.model.android.AndroidAppTarget;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.template.config.TransformBuckFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.gradle.api.Project;

public final class TransformManager {

  private static final String TRANSFORM_CACHE =
      OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/transform";
  public static final String TRANSFORM_RULE = "//" + TRANSFORM_CACHE + ":okbuck_transform";

  public static final String CONFIGURATION_TRANSFORM = "transform";
  private static final String TRANSFORM_FOLDER = "transform/";
  private static final String TRANSFORM_JAR = "transform-cli-1.1.0.jar";
  private static final String TRANSFORM_JAR_RULE = "//" + TRANSFORM_CACHE + ":" + TRANSFORM_JAR;

  private static final String OPT_TRANSFORM_CLASS = "transform";
  private static final String OPT_CONFIG_FILE = "configFile";
  private static final String PREFIX =
      "java -Dokbuck.inJarsDir=$IN_JARS_DIR -Dokbuck.outJarsDir=$OUT_JARS_DIR "
          + "-Dokbuck.androidBootClasspath=$ANDROID_BOOTCLASSPATH ";
  private static final String SUFFIX =
      "-cp $(location "
          + TransformManager.TRANSFORM_RULE
          + ") "
          + "com.uber.okbuck.transform.CliTransform; ";

  private final Project rootProject;

  private ImmutableSet<String> deps;

  public TransformManager(Project rootProject) {
    this.rootProject = rootProject;
  }

  public void fetchTransformDeps() {
    File cacheDir =
        DependencyUtils.createCacheDir(rootProject, OkBuckGradlePlugin.EXTERNAL_DEPENDENCY_CACHE);
    DependencyCache dependencyCache =
        new DependencyCache(rootProject, cacheDir, ProjectUtil.getDependencyManager(rootProject));

    Scope transformScope =
        Scope.builder(rootProject)
            .configuration(CONFIGURATION_TRANSFORM)
            .depCache(dependencyCache)
            .build();

    ImmutableSet.Builder<String> depsBuilder =
        new ImmutableSet.Builder<String>()
            .addAll(BuckRuleComposer.targets(transformScope.getTargetDeps()));

    depsBuilder.addAll(BuckRuleComposer.external(transformScope.getExternalDeps()));

    depsBuilder.add(TRANSFORM_JAR_RULE);

    deps = depsBuilder.build();
  }

  public void finalizeDependencies() {
    Path cacheDir = rootProject.file(TRANSFORM_CACHE).toPath();
    new TransformBuckFile().transformJar(TRANSFORM_JAR).deps(deps).render(cacheDir.resolve("BUCK"));
    FileUtil.copyResourceToProject(
        TRANSFORM_FOLDER + TRANSFORM_JAR, new File(cacheDir.toFile(), TRANSFORM_JAR));
  }

  public static Pair<String, List<String>> getBashCommandAndTransformDeps(AndroidAppTarget target) {
    List<Pair<String, String>> results =
        target
            .getTransforms()
            .stream()
            .map(it -> getBashCommandAndTransformDeps(target, it))
            .collect(Collectors.toList());
    return Pair.of(
        String.join(" ", results.stream().map(Pair::getLeft).collect(Collectors.toList())),
        results.stream().map(Pair::getRight).filter(Objects::nonNull).collect(Collectors.toList()));
  }

  private static Pair<String, String> getBashCommandAndTransformDeps(
      AndroidAppTarget target, Map<String, String> options) {
    String transformClass = options.get(OPT_TRANSFORM_CLASS);
    String configFile = options.get(OPT_CONFIG_FILE);
    StringBuilder bashCmd = new StringBuilder(PREFIX);

    String configFileRule = null;
    if (transformClass != null) {
      bashCmd.append("-Dokbuck.transformClass=");
      bashCmd.append(transformClass);
      bashCmd.append(" ");
    }
    if (configFile != null) {
      configFileRule =
          getTransformConfigRuleForFile(
              target.getProject(), target.getRootProject().file(configFile));
      bashCmd.append("-Dokbuck.configFile=$(location ");
      bashCmd.append(configFileRule);
      bashCmd.append(") ");
    }
    bashCmd.append(SUFFIX);
    return Pair.of(bashCmd.toString(), configFileRule);
  }

  private static String getTransformConfigRuleForFile(Project project, File config) {
    String path = getTransformFilePathForFile(project, config);
    File configFile =
        new File(project.getRootDir(), TransformManager.TRANSFORM_CACHE + File.separator + path);
    try {
      Files.copy(config.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException ignored) {
    }
    return "//" + TransformManager.TRANSFORM_CACHE + ":" + path;
  }

  private static String getTransformFilePathForFile(Project project, File config) {
    return FileUtil.getRelativePath(project.getRootDir(), config).replaceAll("/", "_");
  }
}
