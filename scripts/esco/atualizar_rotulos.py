"""Confere ou atualiza os rótulos ESCO de um arquivo de vocabulário.

Busca, na API oficial da ESCO, o rótulo preferido em português e em inglês de
cada competência que tem `esco.uri`, e compara com o que está no arquivo.

Uso:
    python scripts/esco/atualizar_rotulos.py src/main/resources/vocabulario/java-spring.json
    python scripts/esco/atualizar_rotulos.py --gravar src/main/resources/vocabulario/java-spring.json

Só usa a biblioteca padrão. Dados ESCO: © União Europeia, https://esco.ec.europa.eu
"""

import argparse
import json
import sys
import urllib.parse
import urllib.request

API = "https://ec.europa.eu/esco/api/resource/skill"


def buscar_rotulos(uri: str) -> tuple[str, str]:
    consulta = urllib.parse.urlencode({"uri": uri, "language": "pt"})
    with urllib.request.urlopen(f"{API}?{consulta}", timeout=30) as resposta:
        dados = json.load(resposta)
    rotulos = dados["preferredLabel"]
    return rotulos["pt"], rotulos["en"]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("arquivo")
    parser.add_argument("--gravar", action="store_true", help="grava os rótulos atualizados no arquivo")
    args = parser.parse_args()

    with open(args.arquivo, encoding="utf-8") as f:
        vocabulario = json.load(f)

    divergencias = 0
    for competencia in vocabulario["competencias"]:
        esco = competencia.get("esco")
        if not esco:
            continue
        pt, en = buscar_rotulos(esco["uri"])
        if (esco.get("rotuloPt"), esco.get("rotuloEn")) != (pt, en):
            divergencias += 1
            print(f"{competencia['id']}: {esco.get('rotuloPt')!r} / {esco.get('rotuloEn')!r} -> {pt!r} / {en!r}")
            esco["rotuloPt"], esco["rotuloEn"] = pt, en

    if divergencias and args.gravar:
        with open(args.arquivo, "w", encoding="utf-8", newline="\n") as f:
            json.dump(vocabulario, f, ensure_ascii=False, indent=2)
            f.write("\n")
        print(f"{divergencias} competência(s) atualizada(s).")
    elif divergencias:
        print(f"{divergencias} divergência(s). Rode com --gravar para atualizar.")
        return 1
    else:
        print("Rótulos ESCO em dia.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
