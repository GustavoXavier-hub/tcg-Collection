# TCG Collection

App Android (Java) para organizar a coleção de cartas de TCG — vários jogos,
começando por One Piece. Você digita o código da carta, o app busca os dados
e a imagem numa API pública e guarda tudo localmente. Sem IA em runtime, sem
backend, sem custo mensal — local-first.

## Funcionalidades (v0.1)

- **Cadastro pelo código** — aceita formatos variados (`OP12-034`, `op12 34`,
  `ST10-1`...) e normaliza antes de buscar na OPTCG API.
- **Versões / arte alternativa** — quando a carta tem variações (`_p1`,
  `_p2`), aparece um seletor para escolher qual você tem.
- **Detalhes do exemplar** — quantidade, idioma (EN/JP/PT/Outro), condição
  (NM..DMG) e preço pago opcional.
- **Repetidas somam** — a mesma carta com mesmo idioma e condição soma na
  quantidade em vez de duplicar a linha.
- **Lista com resumo** — toque na carta para +1, −1 ou remover (remover
  sempre pede confirmação).
- **Cache** — dados da carta ficam no SQLite (cada carta é buscada uma vez só)
  e imagens em cache de memória + disco.
- **Modo escuro** — segue o tema do sistema.

## Stack técnica

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| UI | AppCompat, Material, RecyclerView |
| Persistência | SQLite puro (sem Room) |
| Dados das cartas | [OPTCG API](https://optcgapi.com) (HTTPS) |
| Testes | JUnit 4 + org.json |

`minSdk 26`, `targetSdk 34`, `compileSdk 34`.

## Estrutura do projeto

```
app/src/main/java/com/gustavo/tcgcollection/
├── data/    # BancoDados (SQLite), ColecaoRepo — tabelas `cartas` e `colecao`
├── fonte/   # Um adaptador por jogo (FonteCartas). Hoje: OnePieceFonte
├── model/   # Carta, ItemColecao
├── net/     # Rede — única classe que acessa a rede, força HTTPS
├── ui/      # MainActivity, AdicionarActivity, ColecaoAdapter, ImagemLoader
└── util/    # Moeda
```

Jogo novo = nova classe em `fonte/` implementando `FonteCartas` e registrada
em `Fontes`. Nada de `android.*` em `fonte/`, para continuar testável no PC.

## Build e testes

```
./gradlew assembleDebug
./gradlew test
```

Os testes de unidade cobrem normalização de código, parse do JSON da API,
formatação de moeda e uma guarda contra regex incompatível com ICU.

## Roadmap

1. Coleção ✅ (One Piece)
2. Decks — listas que cruzam com a coleção e mostram o que falta
3. Preços e gastos — total por deck/jogo, lista de desejos
4. Outros jogos — Magic (Scryfall), Pokémon
