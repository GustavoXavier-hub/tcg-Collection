# TCG Collection

App Android (Java) para organizar a coleção de cartas de TCG — vários jogos,
começando por One Piece. Você digita o código da carta, o app busca os dados
e a imagem numa API pública e guarda tudo localmente. Sem IA em runtime, sem
backend, sem custo mensal — local-first.

## Funcionalidades (v0.2)

- **Cadastro pelo código** — aceita formatos variados (`OP12-034`, `op12 34`,
  `ST10-1`...) e normaliza antes de buscar na OPTCG API.
- **Leitura pela câmera** — botão de câmera ao lado do "Buscar": CameraX +
  ML Kit (reconhecimento de texto no aparelho, offline) acha o código impresso
  na carta, tolerando confusões do OCR (O/0, I/1, S/5, B/8). Só aceita quando o
  mesmo código aparece em 2 quadros seguidos; aí volta ao cadastro e já busca.
  Tem lanterna para cartas com reflexo.
- **Versões / arte alternativa** — quando a carta tem variações (`_p1`,
  `_p2`), aparece um seletor para escolher qual você tem.
- **Detalhes do exemplar** — quantidade, idioma (EN/JP/PT/Outro), condição
  (NM..DMG) e preço pago opcional.
- **Repetidas somam** — a mesma carta com mesmo idioma e condição soma na
  quantidade em vez de duplicar a linha.
- **Menu lateral por set** — filtra a coleção pelo set oficial (nome que a
  API informa, ex.: "Romance Dawn"), com a contagem de cartas de cada um.
- **Faltantes do set** — com um set escolhido, mostra quais números do set
  você ainda não tem (ter qualquer versão da carta conta). O checklist vem de
  `/api/sets/{id}/` ou `/api/decks/{id}/`, fica guardado no banco e só é
  baixado de novo após 30 dias ou no "atualizar". Tocar numa faltante abre o
  cadastro já buscando o código.
- **Etiqueta de tipo de arte** — a API não tem campo de foil/full art; o tipo
  vem como sufixo no nome ("(Alternate Art) (Manga)"). `util/TipoArte` lê os
  sufixos com palavra de arte conhecida e mostra uma etiqueta ("Arte
  alternativa · Mangá", "SP", "Wanted Poster"...) na lista e no cadastro.
  Apelidos como "Mr.1 (Daz.Bonez)" ficam no nome.
- **Lista com resumo** — toque na carta para +1, −1, trocar a versão/tipo de
  arte (busca as versões na API e junta com a linha existente se já tiver a
  mesma versão, idioma e condição) ou remover (remover sempre pede confirmação).
- **Cache** — dados da carta ficam no SQLite (cada carta é buscada uma vez só)
  e imagens em cache de memória + disco.
- **Modo escuro** — segue o tema do sistema.
- **Tema Usopp** — o app é "cuidado" pelo atirador dos Chapéu de Palha:
  paleta madeira do estilingue + amarelo dos óculos de mira, ícone com a
  máscara do Sogeking (vetor original, gerado por `tools/icone.py`, com camada
  monocromática para o ícone temático), splash com frase sorteada e falas dele no
  estado vazio, ao salvar, quando a carta não é encontrada e em erros de rede.
  Easter eggs ao salvar certas cartas (Usopp, Sogeking, Yasopp, Kaya, Merry,
  Luffy, Yonkous) ou 4 cópias de uma vez. Tudo em `util/MugiwaraPersona`.

### Imagens da persona (só locais)

O cartaz da splash e a imagem do estado vazio são arte do anime/fan art, então
**não vão pro repositório**: ficam em `app/src/main/res-local/drawable-nodpi/`
(`persona_splash.jpg`, `persona_vazio.jpg`, `persona_fundo.jpg`), pasta
ignorada pelo Git e somada aos recursos pelo `build.gradle`. O fundo aparece
bem apagado (12%) atrás da lista e do cadastro. Sem as imagens o app compila
igual: a splash mostra o ícone da máscara, o estado vazio fica só com o
texto e o fundo volta à cor de pergaminho.

## Stack técnica

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| UI | AppCompat, Material, RecyclerView |
| Persistência | SQLite puro (sem Room) |
| Dados das cartas | [OPTCG API](https://optcgapi.com) (HTTPS) |
| Câmera / OCR | CameraX, ML Kit Text Recognition (modelo dentro do APK) |
| Testes | JUnit 4 + org.json |

`minSdk 26`, `targetSdk 34`, `compileSdk 34`.

## Estrutura do projeto

```
app/src/main/java/com/gustavo/tcgcollection/
├── data/    # BancoDados (SQLite), ColecaoRepo — tabelas `cartas`, `colecao`,
│            # `sets_baixados` e `set_checklist`
├── fonte/   # Um adaptador por jogo (FonteCartas). Hoje: OnePieceFonte
├── model/   # Carta, ItemColecao
├── net/     # Rede — única classe que acessa a rede, força HTTPS
├── ui/      # SplashActivity, MainActivity (lista + menu de sets),
│            # FaltantesActivity, AdicionarActivity, ScannerActivity,
│            # ColecaoAdapter, ImagemLoader, PersonaImagens
└── util/    # Moeda, MugiwaraPersona (frases e easter eggs do Usopp),
             # TipoArte (etiqueta de arte a partir do nome)

tools/icone.py  # gera o ícone (máscara do Sogeking) e uma prévia em 48 px
```

Jogo novo = nova classe em `fonte/` implementando `FonteCartas` e registrada
em `Fontes`. Nada de `android.*` em `fonte/`, para continuar testável no PC.

## Build e testes

```
./gradlew assembleDebug
./gradlew test
```

Os testes de unidade cobrem normalização de código, parse do JSON da API,
o filtro do checklist do set,
formatação de moeda, as frases/easter eggs da persona e uma guarda contra
regex incompatível com ICU.

## Roadmap

1. Coleção ✅ (One Piece)
2. Decks — listas que cruzam com a coleção e mostram o que falta
3. Preços e gastos — total por deck/jogo, lista de desejos
4. Outros jogos — Magic (Scryfall), Pokémon
