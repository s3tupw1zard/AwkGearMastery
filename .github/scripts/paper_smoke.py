"""Boot an isolated loopback-only server and verify plugin enablement and clean stop."""
import hashlib
import json
from pathlib import Path
import queue
import shutil
import subprocess
import sys
import threading
import time
import urllib.request

USER_AGENT = "AwkGearMastery-CI/1.0 (https://github.com/s3tupw1zard/AwkGearMastery)"


def fetch(url):
    return urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": USER_AGENT}), timeout=90)


def main():
    version, artifact_dir = sys.argv[1:]
    jars = list(Path(artifact_dir).glob("*.jar"))
    if len(jars) != 1:
        raise RuntimeError(f"Expected one distributable JAR, found {jars}")
    root = Path("smoke-server")
    (root / "plugins").mkdir(parents=True, exist_ok=True)
    shutil.copy2(jars[0], root / "plugins" / jars[0].name)
    with fetch(f"https://fill.papermc.io/v3/projects/paper/versions/{version}/builds") as response:
        builds = json.load(response)
    stable = [build for build in builds if build["channel"] == "STABLE"]
    if not stable:
        raise RuntimeError(f"No stable Paper build for the exact requested version {version}")
    build = max(stable, key=lambda entry: entry["id"])
    download = build["downloads"]["server:default"]
    with fetch(download["url"]) as response:
        data = response.read()
    expected = download.get("checksums", {}).get("sha256")
    if expected and hashlib.sha256(data).hexdigest() != expected:
        raise RuntimeError("Paper server checksum mismatch")
    (root / "server.jar").write_bytes(data)
    (root / "eula.txt").write_text("eula=true\n")
    (root / "server.properties").write_text(
        "server-ip=127.0.0.1\nserver-port=25565\nonline-mode=false\n"
        "view-distance=2\nsimulation-distance=2\nmax-players=1\n"
        "level-type=minecraft:normal\ngenerate-structures=false\n"
        "enable-rcon=false\nspawn-protection=0\n"
    )
    print(f"Testing {jars[0].name} on Paper {version} build {build['id']}", flush=True)
    process = subprocess.Popen(
        ["java", "-Xms512M", "-Xmx1536M", "-jar", "server.jar", "--nogui"],
        cwd=root, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT, text=True, bufsize=1,
    )
    output = queue.Queue()

    def read_output():
        for line in process.stdout:
            output.put(line)
        output.put(None)

    threading.Thread(target=read_output, daemon=True).start()
    enabled = ready = stopped = help_seen = False
    deadline = time.monotonic() + 240
    try:
        with (root / "console.log").open("w") as log:
            while time.monotonic() < deadline:
                try:
                    line = output.get(timeout=1)
                except queue.Empty:
                    continue
                if line is None:
                    break
                log.write(line)
                log.flush()
                print(line, end="", flush=True)
                help_seen |= "AwkGearMastery Command Help:" in line
                enabled |= "Successfully started AwkGearMastery v" in line
                ready |= 'Done (' in line and 'For help' in line
                if enabled and ready and not stopped:
                    process.stdin.write("awkgearmastery help\nstop\n")
                    process.stdin.flush()
                    stopped = True
            if not (enabled and ready and stopped):
                raise RuntimeError(f"Plugin/server did not start successfully on {version}")
            process.wait(timeout=30)
            if not help_seen:
                raise RuntimeError("The plugin help command did not complete successfully")
            if process.returncode != 0:
                raise RuntimeError(f"Server stopped with exit code {process.returncode}")
    finally:
        if process.poll() is None:
            process.kill()
            process.wait()


if __name__ == "__main__":
    main()
