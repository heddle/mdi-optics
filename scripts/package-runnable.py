"""Create the no-Maven distribution from a verified Maven build."""

import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
from tempfile import TemporaryDirectory
from xml.etree import ElementTree
from zipfile import ZipFile


def package(expected_tag):
    pom = ElementTree.parse("pom.xml")
    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
    artifact = pom.findtext("m:artifactId", namespaces=namespace)
    version = pom.findtext("m:version", namespaces=namespace)
    main_class = pom.findtext("m:properties/m:exec.mainClass", namespaces=namespace)
    # Without a tag this is a rehearsal, which may package a snapshot.
    pattern = r"\d+\.\d+\.\d+" if expected_tag else r"\d+\.\d+\.\d+(-SNAPSHOT)?"
    if not re.fullmatch(pattern, version):
        raise SystemExit(f"Set a release version in pom.xml before packaging: {version}")
    if expected_tag and expected_tag != f"v{version}":
        raise SystemExit(f"Tag {expected_tag} does not match Maven version {version}")

    target = Path("target").resolve()
    name = f"{artifact}-{version}"
    dependencies = [Path(entry) for entry in (target / "runtime-classpath.txt").read_text().strip().split(os.pathsep)]
    if len({dependency.name for dependency in dependencies}) != len(dependencies):
        raise SystemExit("Runtime dependencies have duplicate filenames")

    with TemporaryDirectory(dir=target) as staging:
        staging = Path(staging)
        folder = staging / name
        lib = folder / "lib"
        lib.mkdir(parents=True)
        licenses = folder / "licenses"
        shutil.copytree("docs/runtime-licenses", licenses)
        shutil.copy2("LICENSE", folder / "LICENSE.txt")
        for dependency in dependencies:
            shutil.copy2(dependency, lib)
            with ZipFile(dependency) as jar:
                for entry in jar.namelist():
                    if re.fullmatch(r"META-INF/(LICENSE|NOTICE)(\.txt)?", entry, re.IGNORECASE):
                        notice = f"{dependency.stem}-{Path(entry).stem.upper()}.txt"
                        (licenses / notice).write_bytes(jar.read(entry))

        launcher = folder / "mdi-optics.jar"
        shutil.copy2(target / f"{name}.jar", launcher)
        classpath = "Class-Path: " + " ".join(f"lib/{dependency.name}" for dependency in dependencies)
        # Continuation lines begin with a space that the manifest reader removes.
        manifest = staging / "launcher.mf"
        lines = [classpath[:70]]
        lines.extend(" " + classpath[index:index + 69] for index in range(70, len(classpath), 69))
        manifest.write_text(f"Main-Class: {main_class}\n" + "\n".join(lines) + "\n", encoding="ascii")
        subprocess.run(["jar", "--update", "--file", launcher, "--manifest", manifest], check=True)

        (folder / "README.txt").write_text(f"""Light Bench v{version}

Extract the entire ZIP and open a terminal in the extracted {name} folder.
With Java 17 or newer installed, run:

    java -jar mdi-optics.jar

Keep the lib folder next to the JAR. No Maven build is required.
""", encoding="utf-8")
        archive = shutil.make_archive(str(target / f"{name}-runnable"), "zip", staging, name)
        with ZipFile(archive) as zipped:
            if zipped.testzip() is not None:
                raise SystemExit(f"ZIP integrity check failed: {archive}")
        print(archive)


if __name__ == "__main__":
    package(sys.argv[1] if len(sys.argv) > 1 else "")
