package com.squareup.sort

import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static com.squareup.test.Runner.build
import static com.squareup.test.Runner.buildAndFail

final class FunctionalSpec extends Specification {

  private static final String REPO = System.getProperty('rootDir').with {
    Paths.get(it, 'app/build/for-tests').toString().replace('\\', '\\\\')
  }

  @TempDir
  Path dir

  def "can configure program version"() {
    given: 'a build script that sets program version explicitly'
    // nb: we can't easily use an earlier version because 0.2 doesn't support --verbose. The plugin code
    // would have to be modified to be version/feature-aware.
    def version = '0.3'
    def buildScript = dir.resolve('build.gradle')
    Files.writeString(buildScript, buildScriptWithVersion(version))

    when: 'We sort dependencies'
    build(dir, 'sortDependencies')

    then: 'Dependencies are sorted'
    buildScript.text == """\
      plugins {
        id 'java-library'
        id 'com.squareup.sort-dependencies'
      }

      sortDependencies {
        version = '$version'
      }

      repositories {
        mavenCentral()
        maven { url '$REPO' }
      }

      dependencies {
        implementation(platform('com.squareup.okhttp3:okhttp-bom:4.10.0'))
        implementation('com.squareup.okhttp3:okhttp:4.10.0')
        implementation('com.squareup.okio:okio:3.2.0')
      }
    """.stripIndent()
  }

  def "can sort build.gradle"() {
    given: 'A build script with unsorted dependencies'
    def buildScript = dir.resolve('build.gradle')
    Files.writeString(buildScript, BUILD_SCRIPT)

    when: 'We sort dependencies'
    build(dir, 'sortDependencies')

    then: 'Dependencies are sorted'
    buildScript.text == """\
      plugins {
        id 'java-library'
        id 'com.squareup.sort-dependencies'
      }

      repositories {
        mavenCentral()
        maven { url '$REPO' }
      }

      dependencies {
        implementation(platform('com.squareup.okhttp3:okhttp-bom:4.10.0'))
        implementation('com.squareup.okhttp3:okhttp:4.10.0')
        implementation('com.squareup.okio:okio:3.2.0')
      }
    """.stripIndent()
  }

  def "can sort build.gradle.kts"() {
    given: 'A build script with unsorted dependencies'
    def buildScript = dir.resolve('build.gradle.kts')
    Files.writeString(buildScript, BUILD_SCRIPT_KTS)

    when: 'We sort dependencies'
    build(dir, 'sortDependencies')

    then: 'Dependencies are sorted'
    buildScript.text == """\
      plugins {
        `java-library`
        id("com.squareup.sort-dependencies")
      }

      repositories {
        mavenCentral()
        maven { url = uri("$REPO") }
      }

      dependencies {
        implementation(platform("com.squareup.okhttp3:okhttp-bom:4.10.0"))
        implementation("com.squareup.okhttp3:okhttp:4.10.0")
        implementation("com.squareup.okio:okio:3.2.0")
      }
    """.stripIndent()
  }

  def "can check sort order"() {
    given: 'A build script with unsorted dependencies'
    def buildScript = dir.resolve('build.gradle')
    Files.writeString(buildScript, BUILD_SCRIPT)

    when: 'We sort dependencies'
    def result = buildAndFail(dir, 'checkSortDependencies', '--verbose')

    then: 'Dependencies are not sorted'
    result.output.contains('1 scripts are not ordered correctly.')
  }

  private static final BUILD_SCRIPT = """\
    plugins {
      id 'java-library'
      id 'com.squareup.sort-dependencies'
    }

    repositories {
      mavenCentral()
      maven { url '$REPO' }
    }

    dependencies {
      implementation('com.squareup.okio:okio:3.2.0')
      implementation('com.squareup.okhttp3:okhttp:4.10.0')
      implementation(platform('com.squareup.okhttp3:okhttp-bom:4.10.0'))
    }
  """.stripIndent()

  private String buildScriptWithVersion(String version) {
    """\
      plugins {
        id 'java-library'
        id 'com.squareup.sort-dependencies'
      }

      sortDependencies {
        version = '$version'
      }

      repositories {
        mavenCentral()
        maven { url '$REPO' }
      }

      dependencies {
        implementation('com.squareup.okio:okio:3.2.0')
        implementation('com.squareup.okhttp3:okhttp:4.10.0')
        implementation(platform('com.squareup.okhttp3:okhttp-bom:4.10.0'))
      }
    """.stripIndent()
  }

  private static final BUILD_SCRIPT_KTS = """\
    plugins {
      `java-library`
      id("com.squareup.sort-dependencies")
    }

    repositories {
      mavenCentral()
      maven { url = uri("$REPO") }
    }

    dependencies {
      implementation("com.squareup.okio:okio:3.2.0")
      implementation("com.squareup.okhttp3:okhttp:4.10.0")
      implementation(platform("com.squareup.okhttp3:okhttp-bom:4.10.0"))
    }
  """.stripIndent()
}
