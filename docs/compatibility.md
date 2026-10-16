# Server compatibility

- Recommended and gameplay-tested: Paper/Purpur 26.3.
- Minimum API and compile dependency: Paper 1.21.11.
- Required Java runtime: 25 on all server versions, including 1.21.11.
- The same packaged JAR is startup-tested in CI on Paper 1.21.11, 26.1.2, 26.2, 26.3.
- Older APIs are not supported; startup success is not full gameplay certification.

`plugin.yml` declares the oldest supported API rather than the recommended server.
Compiling against the oldest API prevents accidentally depending on newer API
methods merely because the loader flag was lowered. Existing material names are
resolved at runtime; unsupported materials in item profiles are skipped.

The smoke check uses an isolated loopback-only test server and never opens a
public server. It verifies enablement, waits for server readiness, requests the
plugin help command and performs a clean stop. Inspect startup artifacts for
profile warnings and errors; in-game checks remain necessary on older versions.

To run tests with a different Paper API on the test classpath:

```sh
./gradlew clean build -PpaperTestApi=26.2.build.+
```

The production compile API remains 1.21.11 regardless of that test setting.
