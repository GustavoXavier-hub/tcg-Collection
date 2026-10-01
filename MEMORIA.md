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

### Sessão 2 — tema Usopp
- Persona Usopp (`util/MugiwaraPersona`): splash, estado vazio, salvar (Snackbar),
  não encontrada, erro de rede, easter eggs por nome da carta / 4 cópias.
- Paleta madeira `#8A4B14` + amarelo `#F2B33D`; ícone = máscara do Sogeking em vetor
  (`tools/icone.py` gera foreground + monochrome; conferir prévia em 48 px).
- Imagens do anime (cartaz WANTED, Sogeking, fundo 12% na lista/cadastro) em `res-local/`, FORA do Git
  (repo público). `PersonaImagens` busca por nome e cai no ícone sem elas.

### Sessão 3 — sets e faltantes
- Menu lateral (DrawerLayout + NavigationView) filtra por set (`cartas.colecao` = set_name).
- Banco v2: `cartas.set_id` + tabelas `sets_baixados` e `set_checklist`.
  Cartas salvas na v1 ficam sem set_id → set aparece no menu, mas sem "faltantes"
  até salvar uma carta do set de novo.
- Faltantes = checklist (arte normal, prefixo do próprio set) − códigos que tenho
  em qualquer versão. Checklist em cache por 30 dias.

### Sessão 4 — tipo de arte
- `util/TipoArte`: etiqueta a partir do sufixo do nome; só parêntese com palavra de
  arte conhecida (lista PALAVRAS_DE_ARTE). Sem mudança no banco.
- "Trocar versão / tipo de arte" no toque da carta (`ColecaoRepo.trocarVersao`, junta
  linhas se bater no índice único).

### Sessão 5 — leitura pela câmera
- `ScannerActivity` (CameraX + ML Kit text-recognition 16.0.1, versões da Estante).
- `FonteCartas.acharCodigoNoTexto` — regex frouxa CODIGO_OCR + troca de letras por
  dígitos; exige hífen antes do número. 2 leituras iguais seguidas para aceitar.
- APK debug foi a ~48 MB por causa do modelo bundled do ML Kit.

## Próximos pilares (ideia do Gustavo)
1. Coleção ✅ (One Piece)
2. Decks — listas que cruzam com a coleção e mostram o que falta
3. Preços e gastos — total por deck/jogo, lista de desejos
4. Outros jogos — Magic (Scryfall), Pokémon

## Lições permanentes
- OPTCG API: dados só da versão em INGLÊS; ST usa `/api/decks/card/{id}/`,
  o resto `/api/sets/card/{id}/`. Servidor pago do bolso pelo dono → buscar cada carta uma vez.
- set_name NÃO é chave confiável: a carta diz "Premium Booster -The Best-" e
  `/api/allSets/` diz "Premium Booster - The Best". Para buscar set, usar set_id.
- Checklist de set: `/api/sets/OP-05/` (~100 KB, traz alt arts e reimpressões —
  filtrar por arte normal + prefixo OP05-); starter decks em `/api/decks/ST-01/`.
- Parênteses no nome nem sempre são arte: "Mr.1 (Daz.Bonez)", "Miss Doublefinger (Zala)".
  Levantamento do OP-05 (154 cartas): Alternate Art, SP, Manga, Gold-Stamped Signature.
- Nomes dos campos do JSON ficam como constantes `F_*` em `OnePieceFonte`.
  Se a API mudar, mexer só ali e atualizar `OnePieceParseTest`.
- Regex: nada de `\b` nem `(?U)` (ICU). Guarda em `RegexIcuGuardTest`.
