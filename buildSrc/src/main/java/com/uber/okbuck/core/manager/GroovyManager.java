package com.uber.okbuck.core.manager;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.DependencyUtils;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.template.config.Groovyc;
import com.uber.okbuck.template.config.StartGroovy;
import groovy.lang.GroovySystem;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public final class GroovyManager {

  private static final String GROOVY_DEPS_CONFIG = "okbuck_groovy_deps";

  public static final String GROOVY_HOME_LOCATION =
      OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/groovy_installation";

  private final Project rootProject;

  public GroovyManager(Project rootProject) {
    this.rootProject = rootProject;
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void setupGroovyHome() {
    String groovyVersion = GroovySystem.getVersion();

    Configuration groovyConfig = rootProject.getConfigurations().maybeCreate(GROOVY_DEPS_CONFIG);
    rootProject
        .getDependencies()
        .add(GROOVY_DEPS_CONFIG, "org.codehaus.groovy:groovy:" + groovyVersion);
    Set<String> dependencies =
        new DependencyCache(
                rootProject,
                DependencyUtils.createCacheDir(
                    rootProject, OkBuckGradlePlugin.EXTERNAL_DEPENDENCY_CACHE),
                ProjectUtil.getDependencyManager(rootProject))
            .build(groovyConfig);

    File groovyHome = rootProject.file(GROOVY_HOME_LOCATION);

    File groovyStarterConf = new File(groovyHome, "conf/groovy-starter.conf");
    FileUtil.copyResourceToProject("groovy/conf/groovy-starter.conf", groovyStarterConf);

    File groovyc = new File(groovyHome, "bin/groovyc");
    new Groovyc().groovyVersion(groovyVersion).render(groovyc);
    groovyc.setExecutable(true);

    File startGroovy = new File(groovyHome, "bin/startGroovy");
    new StartGroovy().groovyVersion(groovyVersion).render(startGroovy);
    startGroovy.setExecutable(true);

    Path groovyLibCache = rootProject.file(GROOVY_HOME_LOCATION).toPath().resolve("lib");
    FileUtil.deleteQuietly(groovyLibCache);
    groovyLibCache.toFile().mkdirs();

    dependencies.forEach(
        dependency -> {
          Path fromPath = rootProject.file(dependency + ".jar").toPath();
          Path toPath =
              groovyLibCache.resolve(
                  Paths.get(dependency).getFileName().toString() + "-" + groovyVersion + ".jar");

          try {
            toPath.toFile().getParentFile().mkdirs();
            Files.createLink(toPath, fromPath.toRealPath());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  public void finalDependencies() {}
}
