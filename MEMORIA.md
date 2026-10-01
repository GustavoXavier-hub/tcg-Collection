# MEMORIA.md — tcg-collection

App Android (Java 17) para coleção de cartas de TCG, vários jogos.
Pacote: `com.gustavo.tcgcollection` · Pasta: `C:\Projetos\tcg-collection`
Princípios da família: sem IA em runtime, sem backend, sem custo mensal, local-first.

## Arquitetura

- `fonte/` — um adaptador por jogo (`FonteCartas`). Hoje: `OnePieceFonte` (OPTCG API).
  Jogo novo = nova classe + registrar em `Fontes`. Nada de `android.*` aqui (testável no PC).
- `net/Rede` — ÚNICA classe que acessa a rede. Força HTTPS.
- `data/` — SQLite puro (sem Room). Tabelas `cartas` (cache dos dados da API,
  PK jogo+versao) e `colecao` (o que eu tenho: qtd, idioma, condição, preço pago).
  Índice único (jogo, versao, idioma, condicao): repetir = somar quantidade.
- `ui/` — MainActivity (lista + resumo), AdicionarActivity (código → busca → salvar),
  ImagemLoader (cache memória + disco em cacheDir/imagens).

## Histórico

### Sessão 1 — esqueleto + feature 1
- Cadastro de carta One Piece pelo código (OP12-034, op12 34, ST10-1...).
- Versões (arte alternativa `_p1`, `_p2`) aparecem num seletor quando existem.
- Quantidade, idioma (EN/JP/PT/Outro), condição (NM..DMG), preço pago opcional.
- Toque na carta da lista: +1, −1, remover (remover sempre confirma).
- 30 testes de unidade (código, parse do JSON, moeda, guarda ICU).

## Próximos pilares (ideia do Gustavo)
1. Coleção ✅ (One Piece)
2. Decks — listas que cruzam com a coleção e mostram o que falta
3. Preços e gastos — total por deck/jogo, lista de desejos
4. Outros jogos — Magic (Scryfall), Pokémon

## Lições permanentes
- OPTCG API: dados só da versão em INGLÊS; ST usa `/api/decks/card/{id}/`,
  o resto `/api/sets/card/{id}/`. Servidor pago do bolso pelo dono → buscar cada carta uma vez.
- Nomes dos campos do JSON ficam como constantes `F_*` em `OnePieceFonte`.
  Se a API mudar, mexer só ali e atualizar `OnePieceParseTest`.
- Regex: nada de `\b` nem `(?U)` (ICU). Guarda em `RegexIcuGuardTest`.
