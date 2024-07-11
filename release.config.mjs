const publishCmd = `
./gradlew -PstagingRepositoryId=\${process.env.STAGING_REPO_ID} releaseStagingRepositoryOnMavenCentral || exit 3
./gradlew publishJsPackageToNpmjsRegistry || exit 4
`
import config from 'semantic-release-preconfigured-conventional-commits'
config.plugins.push(
    [
        "@semantic-release/exec",
        {
            "publishCmd": publishCmd,
        }
    ],
    "@semantic-release/github",
    "@semantic-release/git",
)

export default config
