# Planejador Automatizado STRIPS + BFS

Planejador automatizado em Java para o domínio **Blocksworld**, desenvolvido como trabalho prático da disciplina de **Algoritmo Avançado I**. O sistema lê um domínio e um problema escritos em PDDL, instancia as ações disponíveis, executa uma busca em largura (*Breadth-First Search* — BFS) e retorna uma sequência de ações capaz de atingir a meta.

## Descrição

O projeto implementa os fundamentos do **planejamento clássico** baseado em STRIPS (*Stanford Research Institute Problem Solver*):

- Representação de estados por conjuntos de predicados.
- Ações definidas por pré-condições, efeitos positivos e efeitos negativos.
- Separação entre o conhecimento do domínio e a definição do problema.
- Instanciação automática de ações a partir dos objetos declarados.
- Busca em largura para encontrar um plano com o menor número de ações.
- Leitura de ficheiros PDDL em um subconjunto simplificado da linguagem.

O domínio de demonstração é o clássico **Blocksworld**, no qual blocos podem ser empilhados, retirados de pilhas, colocados na mesa e movidos por um agente com uma mão.

## Funcionalidades

- Leitura de ficheiros `domain.pddl` e `problem.pddl`.
- Extração de predicados, objetos, estado inicial, meta e esquemas de ação.
- Instanciação automática de ações de aridade 1 e 2.
- Aplicação de pré-condições e efeitos STRIPS.
- Busca em largura (BFS) com controlo de estados já visitados.
- Reconstrução do plano a partir dos nós explorados.
- Contagem de estados gerados e processados.
- Medição do tempo de execução.
- Validação da meta através da verificação de todos os factos exigidos.

## Estrutura do projeto

```text
planejamento-automatizado/
├── src/
│   ├── StripsPlanner.java
│   ├── domain.pddl
│   └── problem.pddl
├── README.md
└── LICENSE
```


## Requisitos

| Requisito | Versão recomendada |
|---|---|
| Java Development Kit (JDK) | 17 ou superior |
| Sistema operativo | Windows, Linux ou macOS |
| Terminal | PowerShell, CMD, Bash ou Zsh |

O projeto não utiliza bibliotecas externas. Todos os componentes necessários fazem parte da biblioteca padrão de Java.

## Instalação

Clone o repositório:

```bash
git clone https://github.com/Lucasmbunga/planejamento-automatizado.git
cd planejamento-automatizado
```

Aceda à pasta do código-fonte:

```bash
cd src
```

Verifique se o Java está instalado:

```bash
java -version
javac -version
```

## Utilização

Compile o programa:

```bash
javac -Xlint:unchecked StripsPlanner.java
```

Execute o planejador indicando o domínio e o problema:

```bash
java -cp . StripsPlanner domain.pddl problem.pddl
```

No PowerShell, utilize:

```powershell
javac -Xlint:unchecked StripsPlanner.java
java -cp . StripsPlanner domain.pddl problem.pddl
```

## Exemplo de utilização

O problema fornecido tem o seguinte estado inicial:

```lisp
(:init
  (on A B)
  (clear A)
  (onTable B)
  (handEmpty)
)
```

A meta é colocar o bloco `B` sobre o bloco `A`:

```lisp
(:goal
  (and
    (on B A)
  )
)
```

O planejador encontra o seguinte plano:

```text
1. unstack(A,B)
2. putdown(A)
3. pickup(B)
4. stack(B,A)
```

A sequência representa a menor solução possível para este problema, considerando que cada ação tem custo unitário.

## Saída esperada

```text
=== Planejador STRIPS + BFS ===
Domínio: blocksworld
Objetos: [A, B]
Estado inicial: [on(A,B), clear(A), onTable(B), handEmpty]
Meta: [on(B,A)]
Ações instanciadas: 12

Plano encontrado:
1. unstack(A,B)
2. putdown(A)
3. pickup(B)
4. stack(B,A)

Comprimento do plano: 4
Estados gerados: 4
Estados processados: 5
Tempo de execução: 0 ms
```

Os valores de tempo podem variar conforme o equipamento, a versão do JDK e a carga do sistema. O número de estados gerados e processados depende da ordem de expansão das ações, mas o comprimento do plano ótimo deve manter-se igual.

## Como funciona

### 1. Leitura do PDDL

O programa recebe dois ficheiros como argumentos:

```bash
java StripsPlanner domain.pddl problem.pddl
```

O primeiro ficheiro descreve o domínio e o segundo descreve o problema concreto.

### 2. Tokenização

O conteúdo dos ficheiros é convertido numa sequência de tokens. Comentários iniciados por `;` são removidos e os parênteses são tratados como delimitadores estruturais.

Por exemplo:

```lisp
(on A B)
```

é convertido em:

```text
["on", "A", "B"]
```

### 3. Parsing de listas aninhadas

O parser transforma os tokens numa estrutura de listas aninhadas. Isto permite representar expressões PDDL como:

```lisp
(:goal
  (and
    (on B A)
  )
)
```

na forma:

```text
[":goal", ["and", ["on", "B", "A"]]]
```

### 4. Extração do problema

O programa extrai:

- O nome do domínio declarado no problema.
- A lista de objetos.
- O estado inicial.
- O conjunto de factos que compõem a meta.

### 5. Instanciação de ações

Cada esquema de ação é instanciado com todas as combinações possíveis de objetos.

Para o domínio Blocksworld com os objetos `A` e `B`, são criadas 12 ações concretas:

| Esquema | Parâmetros | Instâncias |
|---|---:|---:|
| `unstack` | 2 | 4 |
| `putdown` | 1 | 2 |
| `pickup` | 1 | 2 |
| `stack` | 2 | 4 |
| **Total** | — | **12** |

Apenas as ações cujas pré-condições são satisfeitas podem ser aplicadas durante a procura.

### 6. Busca em largura

A BFS explora os estados por níveis de profundidade. Como todas as ações têm custo unitário, o primeiro plano encontrado é aquele com o menor número de ações.

O algoritmo utiliza:

- Uma fila FIFO para manter a ordem de exploração.
- Um conjunto de estados visitados para evitar ciclos e repetições.
- Uma lista de ações em cada nó para reconstruir o plano final.

## Modelo STRIPS

Cada ação é definida por três conjuntos:

| Componente | Função |
|---|---|
| Pré-condições | Factos necessários para aplicar a ação |
| Efeitos positivos | Factos acrescentados ao estado |
| Efeitos negativos | Factos removidos do estado |

A transição de estado segue a semântica STRIPS:

```text
próximo estado = (estado atual - efeitos negativos) + efeitos positivos
```

Por exemplo, a ação `unstack(A,B)` exige que `A` esteja sobre `B`, que `A` esteja livre e que a mão esteja vazia. Após a execução, `A` passa a estar segurado pela mão e `B` torna-se livre.

## Limitações

Este projeto é uma implementação educacional e não pretende ser um planejador PDDL completo.

### Limitações do leitor PDDL

O leitor suporta apenas o subconjunto necessário para o domínio Blocksworld fornecido:

- Predicados sem tipos.
- Objetos sem declaração de tipos.
- Ações com parâmetros sem tipos.
- Pré-condições positivas.
- Efeitos positivos e negativos.
- Conjunções com `and`.

### Funcionalidades não suportadas

O sistema não suporta, por enquanto:

- Tipos de objetos e parâmetros.
- Constantes de domínio.
- Pré-condições negativas.
- Disjunções (`or`).
- Quantificadores (`forall` e `exists`).
- Efeitos condicionais (`when`).
- Ações durativas.
- Recursos numéricos.
- Funções numéricas.
- Planejamento probabilístico.
- Leitura completa da especificação PDDL.

### Limitações do algoritmo

A BFS é adequada para problemas pequenos, mas pode consumir muita memória em problemas maiores, porque guarda a fronteira de exploração e os estados já visitados.

O crescimento do espaço de estados pode ser exponencial em função do número de objetos e da profundidade da solução. Para instâncias maiores, seriam necessárias heurísticas, busca informada ou técnicas mais avançadas de planejamento.

## Testes recomendados

### Teste 1: problema original

Utilize os ficheiros `domain.pddl` e `problem.pddl` fornecidos. O resultado esperado é um plano de quatro ações.

### Teste 2: meta já satisfeita

Altere a meta para:

```lisp
(:goal
  (and
    (on A B)
  )
)
```

Mantendo o estado inicial original, o planejador deve devolver um plano vazio, pois a meta já está satisfeita.

### Teste 3: problema sem solução

Crie um problema com o seguinte estado inicial:

```lisp
(:init
  (handEmpty)
)
```

Mantendo a meta:

```lisp
(:goal
  (and
    (on B A)
  )
)
```

Nenhuma ação pode ser aplicada, pelo que o programa deve informar que não encontrou solução.

### Teste 4: nova disposição inicial

Utilize o seguinte estado inicial:

```lisp
(:init
  (onTable A)
  (onTable B)
  (clear A)
  (clear B)
  (handEmpty)
)
```

Com a meta:

```lisp
(:goal
  (and
    (on B A)
  )
)
```

O plano mínimo esperado é:

```text
1. pickup(B)
2. stack(B,A)
```

## Possíveis melhorias

- Implementar um parser PDDL mais completo.
- Suportar tipos de objetos e parâmetros.
- Implementar busca em profundidade (DFS).
- Implementar busca gulosa (*Greedy Best-First Search*).
- Implementar A* com heurísticas admissíveis.
- Implementar uma heurística para Blocksworld, como o número de blocos fora do lugar.
- Medir memória utilizada durante a execução.
- Criar testes unitários automatizados.
- Validar o plano encontrado de forma independente.
- Suportar ações com custos diferentes.
- Suportar planejamento temporal.
- Suportar planejamento sob incerteza.

## Contribuições

Contribuições são bem-vindas. Para sugerir melhorias:

1. Faça um *fork* do repositório.
2. Crie um ramo para a sua funcionalidade:

```bash
git checkout -b feature/nova-funcionalidade
```

3. Faça as alterações necessárias.
4. Execute os testes e valide os resultados.
5. Envie um *pull request* com uma descrição clara das alterações.

## Licença

Este projeto é disponibilizado para fins académicos e educacionais. Antes de publicar o repositório, escolha uma licença adequada, por exemplo:

```text
MIT License
```

Caso o trabalho seja submetido numa instituição de ensino, verifique as regras da disciplina e da universidade relativamente à licença, reutilização e partilha do código.