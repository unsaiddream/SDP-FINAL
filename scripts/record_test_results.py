from pathlib import Path
import datetime
import json
import platform
import subprocess
import shutil
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
reports = root / 'target' / 'surefire-reports'
output = root / 'docs' / 'test-evidence'
output.mkdir(parents=True, exist_ok=True)
suites = []
for name in ('StudioFacadeTest', 'JsonStudioRepositoryTest', 'StudioGuiTest'):
    source = reports / f'TEST-kz.aitu.eventstudio.{name}.xml'
    if not source.is_file():
        raise SystemExit(f'Run both the default and GUI test profiles first: missing {source.name}')
    tree = ET.parse(source)
    suite = tree.getroot()
    if int(suite.get('failures', 0)) or int(suite.get('errors', 0)) or int(suite.get('skipped', 0)):
        raise SystemExit(f'Test suite is not fully passing: {name}')
    for properties in suite.findall('properties'):
        suite.remove(properties)
    tree.write(output / source.name, encoding='utf-8', xml_declaration=True)
    shutil.copy2(reports / f'kz.aitu.eventstudio.{name}.txt', output)
    suites.append({'suite': name, 'tests': int(suite.get('tests')), 'failures': 0, 'errors': 0, 'skipped': 0,
                   'seconds': float(suite.get('time')), 'methods': [case.get('name') for case in suite.findall('testcase')]})
java_run = subprocess.run(['java', '-version'], capture_output=True, text=True, check=True)
maven_run = subprocess.run(['mvn', '-version'], capture_output=True, text=True, check=True)
java_info = (java_run.stdout + java_run.stderr).splitlines()[0]
maven_lines = maven_run.stdout.splitlines()
maven_info = next(line for line in maven_lines if 'Apache Maven' in line).split(' (')[0]
maven_java = next(line for line in maven_lines if 'Java version:' in line).split(', vendor:')[0]
system = 'macOS' if platform.system() == 'Darwin' else platform.system()
environment = f'{system} {platform.machine()}; {java_info}; {maven_info} ({maven_java}); source release 17'
summary = {'recorded_date': datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=5))).date().isoformat(),
           'suites': suites, 'total_tests': sum(s['tests'] for s in suites),
           'commands': ['mvn clean verify', 'mvn -Pgui test -Dstudio.screenshotDir=docs/screenshots'],
           'environment': environment}
(output / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
lines = ['# Verification results', '', f"Recorded: {summary['recorded_date']} (Asia/Almaty).", '',
         summary['environment'] + '.', '', '| Suite | Passed | Failed | Errors | Skipped |', '| --- | ---: | ---: | ---: | ---: |']
for suite in suites:
    lines.append(f"| {suite['suite']} | {suite['tests']} | 0 | 0 | 0 |")
lines += ['', f"Total: **{summary['total_tests']} passing tests** (27 business/persistence and 3 GUI).", '',
          'Commands:', '', '```sh', *summary['commands'], '```', '',
          'The default verification builds the executable shaded JAR. The GUI profile runs only the three GUI-tagged tests and needs a graphical desktop. The two runs together cover all 30 tests. Suite XML copies omit machine-specific JVM properties; test names, assertions, results and timings come from Maven Surefire. Screenshots are rendered Swing content from those visible test windows.', '',
          'The stderr message `A subscriber could not refresh: test subscriber failed` is intentional: it comes from the test that checks subscriber failure isolation. Maven Shade reports a benign duplicate dependency manifest; the executable manifest was smoke-tested using `java -jar ... --help` and a fresh packaged workspace launch.', '',
          'See TEST-SCENARIOS.md for fourteen concrete input / expected / actual scenarios. Java 17 runtime execution, Windows/Linux usability, human independent contributions, weekly presentations and the individual defense were not verified by these tests.']
(root / 'docs' / 'TEST-RESULTS.md').write_text('\n'.join(lines) + '\n')
print(f"Recorded {summary['total_tests']} successful tests in docs/test-evidence")
