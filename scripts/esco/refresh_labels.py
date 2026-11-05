"""Check or refresh the ESCO labels in a vocabulary file.

Fetches, from the official ESCO API, the preferred Portuguese and English
label of every skill that has an `esco.uri`, and compares them with the file.

Usage:
    python scripts/esco/refresh_labels.py src/main/resources/vocabulary/java-spring.json
    python scripts/esco/refresh_labels.py --write src/main/resources/vocabulary/java-spring.json

Standard library only. ESCO data: © European Union, https://esco.ec.europa.eu
"""

import argparse
import json
import sys
import urllib.parse
import urllib.request

API = "https://ec.europa.eu/esco/api/resource/skill"


def fetch_labels(uri: str) -> tuple[str, str]:
    query = urllib.parse.urlencode({"uri": uri, "language": "pt"})
    with urllib.request.urlopen(f"{API}?{query}", timeout=30) as response:
        data = json.load(response)
    labels = data["preferredLabel"]
    return labels["pt"], labels["en"]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("file")
    parser.add_argument("--write", action="store_true", help="write the refreshed labels to the file")
    args = parser.parse_args()

    with open(args.file, encoding="utf-8") as f:
        vocabulary = json.load(f)

    mismatches = 0
    for skill in vocabulary["skills"]:
        esco = skill.get("esco")
        if not esco:
            continue
        pt, en = fetch_labels(esco["uri"])
        current = (esco.get("preferredLabelPt"), esco.get("preferredLabelEn"))
        if current != (pt, en):
            mismatches += 1
            print(f"{skill['id']}: {current[0]!r} / {current[1]!r} -> {pt!r} / {en!r}")
            esco["preferredLabelPt"], esco["preferredLabelEn"] = pt, en

    if mismatches and args.write:
        with open(args.file, "w", encoding="utf-8", newline="\n") as f:
            json.dump(vocabulary, f, ensure_ascii=False, indent=2)
            f.write("\n")
        print(f"{mismatches} skill(s) refreshed.")
    elif mismatches:
        print(f"{mismatches} mismatch(es). Run with --write to refresh them.")
        return 1
    else:
        print("ESCO labels are up to date.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
