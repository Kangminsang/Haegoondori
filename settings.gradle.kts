pluginManagement {
    repositories {
        google()
        // 이 개발 환경에서는 repo.maven.apache.org(mavenCentral() 기본 URL)가 프록시를 통해
        // 자주 429(Too Many Requests)를 반환한다. 완전히 동일한 내용을 미러링하는
        // repo1.maven.org를 명시적으로 사용해 이를 우회한다.
        maven(url = "https://repo1.maven.org/maven2")
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven(url = "https://repo1.maven.org/maven2")
        mavenCentral()
    }
}

rootProject.name = "Hagoondori"

include(":core")
include(":app")
