# API de conteúdos com cache

Atividade sobre cache local. A API cadastra, consulta, atualiza e exclui conteúdos usando Spring Web, Spring Data JPA e H2. As consultas usam um cache com capacidade de 5 itens, TTL de 60 segundos e remoção LRU.

Autor: Willian Batista

Trabalho individual.

## Como executar

É necessário ter JDK 21 ou superior e acesso à internet na primeira execução para baixar o Maven e as dependências. O projeto utiliza a versão estável `4.0.9` do Spring Boot.

Abra um terminal na pasta `hashMap` e execute:

```powershell
.\mvnw.cmd spring-boot:run
```

No Linux ou macOS, use `./mvnw spring-boot:run`. A API fica em `http://localhost:8080`. O H2 funciona em memória: os dados e o cache são perdidos ao encerrar a aplicação.

Para compilar e executar os testes:

```powershell
.\mvnw.cmd clean verify
```

## Como o cache funciona

O `ConteudoCache` é um `@Component` com `Map<Long, EntradaCache>` implementado por `ConcurrentHashMap`. Cada entrada guarda o conteúdo, o instante de armazenamento, o último acesso e uma sequência para ordenar acessos que ocorram no mesmo instante.

Em uma consulta, um item válido gera HIT. Um item ausente ou expirado gera MISS, seguido de uma espera de aproximadamente 2 segundos antes de consultar o banco. O resultado é armazenado no cache. O header `X-Cache` e o log da aplicação informam o resultado da busca.

O TTL é contado desde o armazenamento; um HIT não renova esse prazo. Entradas expiradas são removidas ao serem consultadas e também antes de uma inserção ou da leitura do tamanho atual. Se os 5 espaços estiverem ocupados por itens válidos, a próxima inserção remove o item acessado há mais tempo (LRU).

As operações do cache são sincronizadas para proteger o limite, a escolha do item removido e os contadores. No service, consultas, atualizações e exclusões também são sincronizadas, impedindo que uma busca em andamento recoloque um dado antigo após uma alteração. Essa solução simples faz essas operações aguardarem sua vez quando chegam juntas.

PUT e DELETE invalidam o item após a alteração no banco. `evictions` conta apenas remoções por falta de espaço; TTL, invalidação e limpeza não aumentam esse contador. Hits e misses contam as consultas, inclusive MISS de ID inexistente. O hit ratio vai de 0 a 1 e usa `hits / (hits + misses)`; sem consultas, vale 0. A limpeza remove as entradas e preserva os contadores, que são zerados ao reiniciar a aplicação.

A controller recebe e retorna DTOs e usa o `ConteudoMapper` para converter a entidade. O service recebe e retorna entidades; na consulta, uma função informa HIT ou MISS à controller para que ela preencha o header. O repository usa `JpaRepository`.

## Requisições

| Método | Caminho | Resultado |
| --- | --- | --- |
| POST | `/conteudos` | Cadastra e retorna 201 com o conteúdo e o header Location. |
| GET | `/conteudos/{id}` | Retorna 200 com o conteúdo e `X-Cache: HIT` ou `MISS`. |
| PUT | `/conteudos/{id}` | Atualiza, invalida o cache e retorna 200. |
| DELETE | `/conteudos/{id}` | Exclui, invalida o cache e retorna 204. |
| GET | `/cache/estatisticas` | Retorna tamanhoAtual, limite, hits, misses, evictions e hitRatio. |
| DELETE | `/cache` | Limpa as entradas e retorna 204. |

GET, PUT e DELETE de conteúdo inexistente retornam 404. POST e PUT usam `Content-Type: application/json` e recebem apenas `titulo`, `corpo` e `autor`. O servidor gera `id` e `atualizadoEm`.

## Reprodução opcional da demonstração

A entrega inclui a demonstração executada automaticamente e registrada no [PDF de evidências](output/pdf/evidencias-demonstracao.pdf). A coleção existente foi usada para executar as 31 requisições por HTTP na versão 4.0.9. As instruções abaixo permitem reproduzir os cenários, se necessário.

Para essa reprodução, importe [a coleção](docs/demonstracao.postman_collection.json) pelo botão **Import** do Postman. A variável `baseUrl` aponta para `http://localhost:8080`. As pastas indicam a ordem das requisições e os sete cadastros salvam seus IDs em variáveis da coleção.

1. **Cadastro:** envie os sete POSTs da pasta `01 - Cadastro`. Eles usam o formato abaixo, mudando o número de 1 a 7. O cadastro não preenche o cache.

   ```http
   POST http://localhost:8080/conteudos
   Content-Type: application/json

   {"titulo":"Conteúdo 1","corpo":"Texto do conteúdo 1","autor":"Willian Batista"}
   ```

2. **MISS e HIT:** na pasta `02 - MISS e HIT`, limpe o cache e consulte duas vezes o primeiro conteúdo. A primeira consulta deve mostrar `X-Cache: MISS` e durar cerca de 2 segundos. A segunda deve mostrar `HIT` e ser mais rápida. Compare o tempo exibido no Postman e o campo `tempo` dos logs.

3. **Limite e LRU:** execute a pasta `03 - Limite e LRU` sem pausas longas, para não atingir o TTL. Com o primeiro conteúdo ainda válido no cache, a sequência é:

   | Requisição | O que observar |
   | --- | --- |
   | GET dos conteúdos 2, 3, 4 e 5 | MISS em cada um; o cache chega a 5 itens. |
   | GET `/cache/estatisticas` | `tamanhoAtual` e `limite` iguais a 5. |
   | GET do conteúdo 1 | HIT; passa a ser o mais recente. |
   | GET do conteúdo 6 | MISS; remove o conteúdo 2 por LRU. |
   | GET `/cache/estatisticas` | Tamanho continua em 5; `evictions` aumenta em 1. |
   | GET do conteúdo 1 | HIT; demonstra que ele foi preservado. |
   | GET do conteúdo 2 | MISS; demonstra que ele foi removido e precisa voltar do banco. |

4. **TTL:** na pasta `04 - TTL`, consulte o conteúdo 7 duas vezes: MISS e HIT. Depois do HIT, aguarde pelo menos **61 segundos**, sem consultar esse conteúdo, e envie a terceira consulta. Ela deve gerar MISS e levar aproximadamente 2 segundos. Não é necessário reiniciar a aplicação nem alterar a configuração.

5. **Invalidação após PUT:** execute a pasta `05 - Atualização`. Ela altera o conteúdo 7 com o corpo abaixo. O próximo GET deve gerar MISS e mostrar o texto novo; o seguinte deve gerar HIT.

   ```http
   PUT http://localhost:8080/conteudos/{{id7}}
   Content-Type: application/json

   {"titulo":"Conteúdo 7 atualizado","corpo":"Texto alterado pelo autor","autor":"Willian Batista"}
   ```

6. **Estatísticas finais:** execute `GET {{baseUrl}}/cache/estatisticas`, na pasta `06 - Estatísticas finais`. Confira os seis campos e calcule `hits / (hits + misses)` para comparar com `hitRatio`. Os valores dependem das consultas realizadas desde a inicialização.

7. **Exclusão e limpeza:** a última pasta contém `DELETE /conteudos/{{id7}}`, um GET desse conteúdo (404), `DELETE /cache` e a consulta de estatísticas para conferir tamanho 0.

Nas URLs da coleção, `{{id1}}` até `{{id7}}` representam os IDs retornados no cadastro. Se enviar as requisições fora da coleção, substitua essas variáveis pelos IDs reais.

## Pesquisa

As respostas às seis questões e as referências consultadas estão em [Pesquisa sobre cache](output/pdf/pesquisa-cache.pdf).

## Evidências da entrega

O [PDF de evidências](output/pdf/evidencias-demonstracao.pdf) reúne os sete cadastros, MISS/HIT com tempos e headers, limite e remoção LRU, expiração após uma espera real de 61 segundos, invalidação após PUT, estatísticas e logs correspondentes. Também inclui a compilação e o resumo dos oito testes existentes aprovados.

Arquivos que permitem conferir os resultados:

- [Requisições e respostas completas em JSON](docs/demonstracao-http.json), com horários, headers e tempos medidos.
- [Transcrição da execução](docs/validacao.txt).
- [Logs reais da aplicação](docs/logs-validacao.txt).
- [Saída real da compilação](docs/compilacao.txt).

**Limitação:** Postman e Insomnia não foram encontrados entre os aplicativos disponíveis. Por isso, o PDF contém transcrições HTTP reais e logs como evidência complementar, sem prints dessas ferramentas. A aceitação desse formato em lugar dos prints depende do professor.

## Verificação

O projeto usa Spring Boot 4.0.9, versão estável [publicada no Maven Central](https://spring.io/blog/2026/10/23/spring-boot-4-0-9-available-now/). A compilação de confirmação terminou com `BUILD SUCCESS`. Os oito testes existentes já haviam passado com essa versão e foram reaproveitados; essa compilação usou `-DskipTests` para evitar uma repetição desnecessária.

Os testes em `src/test/java` verificam o contexto Spring, os endpoints, a invalidação, o TTL, o LRU, os contadores e 100 inserções concorrentes em um cache com limite 5. A pesquisa foi conferida e contém o nome Willian Batista, como trabalho individual.

O código Java, os testes e as dependências foram preservados nesta finalização. Os PDFs e os registros estão incluídos na entrega; `target/` permanece ignorado pelo Git. Nenhum commit ou publicação foi realizado.
