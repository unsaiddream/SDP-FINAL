# Verification results

Recorded: 2026-11-16 (Asia/Almaty).

macOS arm64; openjdk version "25.0.1" 2025-10-21; Apache Maven 3.9.16 (Java version: 27); source release 17.

| Suite | Passed | Failed | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| StudioFacadeTest | 21 | 0 | 0 | 0 |
| JsonStudioRepositoryTest | 6 | 0 | 0 | 0 |
| StudioGuiTest | 3 | 0 | 0 | 0 |

Total: **30 passing tests** (27 business/persistence and 3 GUI).

Commands:

```sh
mvn clean verify
mvn -Pgui test -Dstudio.screenshotDir=docs/screenshots
```

The default verification builds the executable shaded JAR. The GUI profile runs only the three GUI-tagged tests and needs a graphical desktop. The two runs together cover all 30 tests. Suite XML copies omit machine-specific JVM properties; test names, assertions, results and timings come from Maven Surefire. Screenshots are rendered Swing content from those visible test windows.

The stderr message `A subscriber could not refresh: test subscriber failed` is intentional: it comes from the test that checks subscriber failure isolation. Maven Shade reports a benign duplicate dependency manifest; the executable manifest was smoke-tested using `java -jar ... --help` and a fresh packaged workspace launch.

See TEST-SCENARIOS.md for fourteen concrete input / expected / actual scenarios. Java 17 runtime execution, Windows/Linux usability, human independent contributions, weekly presentations and the individual defense were not verified by these tests.
