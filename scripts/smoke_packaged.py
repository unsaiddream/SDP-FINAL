from pathlib import Path
import json
import shutil
import subprocess
import tempfile
import time

root = Path(__file__).resolve().parents[1]
java = shutil.which('java')
if not java:
    raise SystemExit('Java is required.')
jar = root / 'target' / 'event-studio.jar'
help_run = subprocess.run([java, '-jar', str(jar), '--help'], capture_output=True, text=True, check=True)
if 'Usage:' not in help_run.stdout:
    raise SystemExit('The executable JAR did not show its usage.')
with tempfile.TemporaryDirectory(prefix='event-studio-smoke-') as temp:
    path = Path(temp) / 'studio.json'
    with (Path(temp) / 'launch.log').open('w+') as log:
        process = subprocess.Popen([java, '-jar', str(jar), str(path)], stdout=log, stderr=log)
        try:
            deadline = time.monotonic() + 15
            while not path.exists() and process.poll() is None and time.monotonic() < deadline:
                time.sleep(0.1)
            if not path.exists() or process.poll() is not None:
                log.seek(0)
                raise SystemExit('Packaged launch failed: ' + log.read())
            time.sleep(0.5)
            if process.poll() is not None:
                raise SystemExit('Application exited during startup.')
            state = json.loads(path.read_text())
            assert len(state['events']) == 2
            assert len(state['participants']) == 3
            assert len(state['bookings']) == 2
            log.seek(0)
            output = log.read()
            if 'Exception' in output or 'could not open' in output:
                raise SystemExit(output)
            print('Packaged JAR passed: --help, visible startup, fresh JSON workspace with 2 events / 3 people / 2 bookings.')
        finally:
            process.terminate()
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()
