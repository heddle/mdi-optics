"""Check the public documentation and download after a GitHub release."""

import json
import re
import sys
import time
from urllib.request import HTTPRedirectHandler, Request, build_opener, urlopen


PLATFORMS = ["Linux", "Windows", "Intel macOS", "Apple Silicon macOS"]
ASSET = "mdi-optics-runnable.zip"
RELEASE_SUMMARY = "The runnable ZIP passed desktop smoke tests with Java 17 and without Maven on:"


class Redirects(HTTPRedirectHandler):
    def __init__(self):
        super().__init__()
        self.destinations = []

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        self.destinations.append(newurl)
        return super().redirect_request(req, fp, code, msg, headers, newurl)


def fetch(url):
    with urlopen(url, timeout=15) as response:
        return response.read().decode("utf-8")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def verify(repository, tag):
    base = f"https://github.com/{repository}/releases"
    release = json.loads(fetch(f"https://api.github.com/repos/{repository}/releases/tags/{tag}"))
    require(release["tag_name"] == tag and not release["draft"], f"{tag} is not public")
    body = release["body"] or ""
    _, summary, after = body.partition(RELEASE_SUMMARY)
    listed = re.match(r"\s*((?:- [^\r\n]+\r?\n?)+)", after)
    platforms = listed.group(1).splitlines() if listed else []
    require(summary and platforms == [f"- {name}" for name in PLATFORMS],
            "Release notes must list all four smoke-test platforms")
    require(any(asset["name"] == ASSET for asset in release["assets"]),
            f"Release is missing {ASSET}")

    links = {
        f"{base}/latest": f"{base}/tag/{tag}",
        f"{base}/latest/download/{ASSET}": f"{base}/download/{tag}/{ASSET}",
    }
    platform_list = ", ".join(PLATFORMS[:-1]) + ", and " + PLATFORMS[-1]
    expected_note = (
        f"Before publication, the release workflow smoke-tests the runnable ZIP on "
        f"{platform_list} with Java 17, launching it without Maven."
    )
    pages = {
        "README": f"https://raw.githubusercontent.com/{repository}/main/README.md",
    }
    for name, url in pages.items():
        page = fetch(url)
        require(expected_note in " ".join(page.split()), f"{name} verification note differs")
        for link in links:
            require(re.search(re.escape(link) + r'["\)]', page), f"{name} is missing {link}")

    for link, destination in links.items():
        redirects = Redirects()
        is_zip = link.endswith("/" + ASSET)
        request = Request(link, headers={"Range": "bytes=0-3"} if is_zip else {})
        with build_opener(redirects).open(request, timeout=15) as response:
            require(destination in redirects.destinations,
                    f"{link} did not redirect through {destination}")
            if is_zip:
                require(response.read(4) == b"PK\x03\x04", f"{link} did not serve a ZIP")
    print(f"README links resolve to {tag} and its runnable ZIP.")
    print(f"Its verification note matches the release notes on {platform_list}.")


if __name__ == "__main__":
    # GitHub's latest redirect and public pages can lag shortly after publication.
    for attempt in range(1, 7):
        try:
            verify(*sys.argv[1:])
            break
        except (OSError, ValueError) as error:
            print(f"Attempt {attempt}: {error}", file=sys.stderr, flush=True)
            if attempt == 6:
                sys.exit(1)
            time.sleep(10)
