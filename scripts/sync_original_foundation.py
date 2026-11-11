import argparse
import hashlib
import json
import re
import subprocess
from pathlib import Path

from lxml import etree

REVISION = "f8a3593fd0c37edca3da9012c0eb4bc37e907cc7"
SOURCE_PACKAGE = "com.mochyy.mochymusic"
TARGET_PACKAGE = "com.arturo254.opentune.foundation"
PREFIX = "original_"


def without_comments(text):
    pattern = r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\x27(?:\\.|[^\x27\\])*\x27|/\*[\s\S]*?\*/|//[^\n]*'
    return re.sub(pattern, lambda m: "" if m[0].startswith(("/*", "//")) else m[0], text)


def generate(source):
    actual = subprocess.check_output(["git", "-C", str(source), "rev-parse", "HEAD"], text=True).strip()
    if actual != REVISION:
        raise ValueError(f"La fuente debe estar en {REVISION}, no {actual}")
    res = source / "app/src/main/res"
    source_files = sorted(p for p in res.rglob("*") if p.is_file())
    excluded = {p for p in source_files if p.parent.name.startswith("xml") or p.name == "resources.properties"}
    selected = [p for p in source_files if p not in excluded]
    symbols = set()
    parser = etree.XMLParser(remove_comments=True)
    trees = {}
    for path in selected:
        folder = path.parent.name.split("-")[0]
        if folder != "values":
            symbols.add((folder, path.name.split(".")[0]))
        if path.suffix == ".xml":
            tree = etree.fromstring(path.read_bytes(), parser)
            trees[path] = tree
            if folder == "values":
                for child in tree:
                    if "name" in child.attrib:
                        kind = child.get("type", child.tag)
                        if kind in ("string-array", "integer-array"):
                            kind = "array"
                        symbols.add((kind, child.get("name")))
            for match in re.finditer(r"@\+id/([\w.]+)", path.read_text()):
                symbols.add(("id", match[1]))

    def reference(match):
        marker, kind, name = match.groups()
        return f"{marker}{kind}/{PREFIX}{name}" if (kind, name) in symbols else match[0]

    def rewrite(value):
        return re.sub(r"(@\+?|\?)([A-Za-z_]+)/([A-Za-z0-9_.]+)", reference, value)

    output = {}
    manifest = []
    for path in selected:
        relative = path.relative_to(res)
        destination = Path("visual-foundation/src/main/res") / relative.parent / (PREFIX + relative.name)
        if path in trees:
            tree = trees[path]
            if path.parent.name.startswith("values"):
                for child in tree:
                    if "name" in child.attrib:
                        child.set("name", PREFIX + child.get("name"))
                    parent = child.get("parent")
                    if parent and ("style", parent) in symbols:
                        child.set("parent", PREFIX + parent)
            for node in tree.iter():
                for key, value in list(node.attrib.items()):
                    node.set(key, rewrite(value))
                if node.text:
                    node.text = rewrite(node.text)
                if node.tail:
                    node.tail = rewrite(node.tail)
            payload = etree.tostring(tree, encoding="utf-8", xml_declaration=True) + b"\n"
        else:
            payload = path.read_bytes()
        output[str(destination)] = payload
        manifest.append({"source": str(path.relative_to(source)), "destination": str(destination), "source_sha256": hashlib.sha256(path.read_bytes()).hexdigest(), "destination_sha256": hashlib.sha256(payload).hexdigest(), "adaptation": "resource_identifiers_and_xml_serialization" if path in trees else "byte_identical"})
    kotlin = source / "app/src/main/kotlin/com/mochyy/mochymusic"
    for path in sorted((kotlin / "ui/theme").glob("*.kt")):
        text = without_comments(path.read_text()).replace(SOURCE_PACKAGE + ".ui.theme", TARGET_PACKAGE + ".theme").replace(SOURCE_PACKAGE + ".constants", TARGET_PACKAGE + ".constants").replace(SOURCE_PACKAGE + ".R", TARGET_PACKAGE + ".R").replace("fun MochyTheme(", "fun OriginalTheme(")
        text = re.sub(r"\bR\.([a-z_]+)\.([\w]+)", lambda m: f"R.{m[1]}.{PREFIX}{m[2]}", text)
        payload = text.lstrip().encode()
        destination = "visual-foundation/src/main/kotlin/com/arturo254/opentune/foundation/theme/" + path.name
        output[destination] = payload
        manifest.append({"source": str(path.relative_to(source)), "destination": destination, "source_sha256": hashlib.sha256(path.read_bytes()).hexdigest(), "destination_sha256": hashlib.sha256(payload).hexdigest(), "adaptation": "packages_resources_entry_name_and_comment_removal"})
    preferences = (kotlin / "constants/PreferenceKeys.kt").read_text()
    enum = re.search(r"enum class PlayerBackgroundStyle\s*\{.*?\n\}", preferences, re.S)[0]
    output["visual-foundation/src/main/kotlin/com/arturo254/opentune/foundation/constants/PlayerBackgroundStyle.kt"] = (f"package {TARGET_PACKAGE}.constants\n\n" + without_comments(enum) + "\n").encode()
    output["visual-foundation/LICENSE"] = (source / "LICENSE").read_bytes()
    output["docs/inventario-02/trasplante.json"] = (json.dumps({"repository": "https://github.com/realidkroo/Mochy-Music", "revision": REVISION, "files": manifest, "excluded": [{"path": str(p.relative_to(source)), "reason": "Configuración Android y widgets: conservar identidad, permisos y servicios del destino"} for p in sorted(excluded)], "symbol_mapping": {f"{kind}/{name}": f"{kind}/{PREFIX}{name}" for kind, name in sorted(symbols)}}, ensure_ascii=False, indent=2) + "\n").encode()
    return output


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    output = generate(args.source.resolve())
    for relative, content in output.items():
        path = root / relative
        if args.check:
            if not path.is_file() or path.read_bytes() != content:
                raise ValueError(f"Diferencia de fidelidad: {relative}")
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(content)
    print(f"{'Verificados' if args.check else 'Trasladados'} {len(output)} archivos desde {REVISION}")


if __name__ == "__main__":
    main()
