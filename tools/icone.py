# Fonte única do ícone (máscara do Sogeking): gera os vector drawables do Android
# e uma prévia HTML em 384/144/48 px.
# Uso: python tools/icone.py app/src/main/res/drawable   (a prévia sai na mesma pasta: apague)
# Viewport 108x108 (adaptive icon). Zona segura: círculo de raio ~33 no centro.
import io, sys

AMARELO = "#F4C430"
AMARELO_ESC = "#D9A21B"
AZUL = "#24377A"
ARO = "#E8E4DA"
LENTE = "#1E5566"
NARIZ = "#F1EEE6"
NARIZ_LINHA = "#B9B2A3"
VERDE = "#2E6B4A"
BOCA = "#C8302E"
CAPA = "#A8242A"
CAPA_SOMBRA = "#7E1A1F"
FUNDO = "#7A4214"
# Aumenta tudo em volta do centro: em 48dp a máscara precisa ocupar mais o círculo.
ESCALA = 1.18
PIVO_Y = 54
DESCE = 5  # a chama não pode encostar na borda do círculo

MASCARA = "M41,42 Q41,33 54,33 Q67,33 67,42 L67,64 Q67,71 60,71 L48,71 Q41,71 41,64 Z"

# (tipo, atributos) na ordem de desenho
camadas = [
    # chamas: uma alta à direita, uma curva à esquerda
    ("path", dict(fill=AMARELO, d="M52,35 C49,27 54,21 63,19 C59,25 63,29 61,35 Z")),
    ("path", dict(fill=AMARELO_ESC, d="M44,40 C37,39 31,35 29,29 C35,32 41,32 46,35 Z")),
    ("path", dict(fill=AMARELO, d=MASCARA)),
    # faixa azul diagonal (recortada pela máscara)
    ("clip", dict(d=MASCARA, inner=[
        ("path", dict(fill=AZUL, d="M38,40 L46,32 L72,58 L64,66 Z")),
    ])),
    # óculos
    ("circle", dict(fill=ARO, cx=47.5, cy=48, r=5.2)),
    ("circle", dict(fill=ARO, cx=60.5, cy=48, r=5.2)),
    ("circle", dict(fill=LENTE, cx=47.5, cy=48, r=3)),
    ("circle", dict(fill=LENTE, cx=60.5, cy=48, r=3)),
    # nariz enfaixado
    ("path", dict(fill=NARIZ, d="M52,41 h4 v17 a2,2 0 0 1 -4,0 z")),
    ("path", dict(stroke=NARIZ_LINHA, sw=0.9, d="M52,45 h4 M52,49 h4 M52,53 h4")),
    # bigode
    ("path", dict(stroke=VERDE, sw=2.6, d="M44,61 Q46,56 51,59")),
    ("path", dict(stroke=VERDE, sw=2.6, d="M64,61 Q62,56 57,59")),
    # boca
    ("path", dict(fill=BOCA, d="M50.5,63.5 Q54,61.5 57.5,63.5 Q54,66.5 50.5,63.5 Z")),
    # capa/cachecol por cima do queixo
    ("path", dict(fill=CAPA, d="M26,92 Q27,72 42,68 Q54,72 66,68 Q81,72 82,92 Z")),
    ("path", dict(stroke=CAPA_SOMBRA, sw=1.6, d="M38,76 Q54,82 70,76 M34,84 Q54,90 74,84")),
]

def vd_item(tipo, a, ind):
    p = " " * ind
    if tipo == "circle":
        cx, cy, r = a["cx"], a["cy"], a["r"]
        d = f"M{cx - r},{cy} a{r},{r} 0 1,0 {2*r},0 a{r},{r} 0 1,0 {-2*r},0"
        return f'{p}<path android:fillColor="{a["fill"]}" android:pathData="{d}" />\n'
    if tipo == "clip":
        s = f'{p}<group>\n{p}    <clip-path android:pathData="{a["d"]}" />\n'
        for t, b in a["inner"]:
            s += vd_item(t, b, ind + 4)
        return s + f"{p}</group>\n"
    if "stroke" in a:
        return (f'{p}<path android:strokeColor="{a["stroke"]}" android:strokeWidth="{a["sw"]}"'
                f' android:strokeLineCap="round" android:pathData="{a["d"]}" />\n')
    return f'{p}<path android:fillColor="{a["fill"]}" android:pathData="{a["d"]}" />\n'

def svg_item(tipo, a, n=[0]):
    if tipo == "circle":
        return f'<circle cx="{a["cx"]}" cy="{a["cy"]}" r="{a["r"]}" fill="{a["fill"]}"/>'
    if tipo == "clip":
        n[0] += 1
        cid = f"c{n[0]}"
        inner = "".join(svg_item(t, b) for t, b in a["inner"])
        return f'<clipPath id="{cid}"><path d="{a["d"]}"/></clipPath><g clip-path="url(#{cid})">{inner}</g>'
    if "stroke" in a:
        return (f'<path d="{a["d"]}" fill="none" stroke="{a["stroke"]}" stroke-width="{a["sw"]}"'
                f' stroke-linecap="round"/>')
    return f'<path d="{a["d"]}" fill="{a["fill"]}"/>'

vd = ('<?xml version="1.0" encoding="utf-8"?>\n'
      "<!-- Máscara do Sogeking (Usopp): desenho original, gerado por tools/icone.py.\n"
      "     Traços grossos e poucas cores: legível em 48dp. -->\n"
      '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
      '    android:width="108dp" android:height="108dp"\n'
      '    android:viewportWidth="108" android:viewportHeight="108">\n')
vd += f'    <group android:scaleX="{ESCALA}" android:scaleY="{ESCALA}" android:pivotX="54" android:pivotY="{PIVO_Y}" android:translateY="{DESCE}">\n'
for t, a in camadas:
    vd += vd_item(t, a, 8)
vd += "    </group>\n</vector>\n"

# Monocromático (ícone temático do Android 13+): silhueta com os óculos vazados.
mono = ('<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Silhueta da máscara do Sogeking para o ícone temático (Android 13+). -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp" android:height="108dp"\n'
        '    android:viewportWidth="108" android:viewportHeight="108">\n'
        f'    <group android:scaleX="{ESCALA}" android:scaleY="{ESCALA}" android:pivotX="54" android:pivotY="{PIVO_Y}" android:translateY="{DESCE}">\n'
        '    <path android:fillColor="#FFFFFFFF" android:fillType="evenOdd"\n'
        f'        android:pathData="{MASCARA} M52,35 C49,27 54,21 63,19 C59,25 63,29 61,35 Z '
        'M42.3,48 a5.2,5.2 0 1,0 10.4,0 a5.2,5.2 0 1,0 -10.4,0 '
        'M55.3,48 a5.2,5.2 0 1,0 10.4,0 a5.2,5.2 0 1,0 -10.4,0" />\n'
        '    <path android:fillColor="#FFFFFFFF" android:pathData="M44,40 C37,39 31,35 29,29 C35,32 41,32 46,35 Z" />\n'
        '    <path android:fillColor="#FFFFFFFF" android:pathData="M30,90 Q31,74 42,70 Q54,74 66,70 Q77,74 78,90 Z" />\n'
        "    </group>\n</vector>\n")

corpo = (f'<g transform="translate(0 {DESCE}) translate(54 {PIVO_Y}) scale({ESCALA}) translate(-54 -{PIVO_Y})">'
         + "".join(svg_item(t, a) for t, a in camadas) + "</g>")
def svg(tam, mascara_circulo):
    clip = '<clipPath id="ic"><circle cx="54" cy="54" r="36"/></clipPath>' if mascara_circulo else ""
    g0, g1 = ('<g clip-path="url(#ic)">', "</g>") if mascara_circulo else ("", "")
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{tam}" height="{tam}" viewBox="18 18 72 72">'
            f'{clip}{g0}<rect x="0" y="0" width="108" height="108" fill="{FUNDO}"/>{corpo}{g1}</svg>')

saida = sys.argv[1]
io.open(saida + "/ic_launcher_foreground.xml", "w", encoding="utf-8", newline="").write(vd)
io.open(saida + "/ic_launcher_monochrome.xml", "w", encoding="utf-8", newline="").write(mono)
html = ('<html><body style="margin:0;background:#ddd;display:flex;gap:24px;align-items:center;padding:24px">'
        + svg(384, True) + svg(144, True) + svg(48, True) + "</body></html>")
io.open(saida + "/previa.html", "w", encoding="utf-8").write(html)
print("ok")
