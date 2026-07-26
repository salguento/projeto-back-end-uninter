# Evidências de testes

Esta pasta preserva os resumos verificáveis das execuções realizadas em 22 de julho de 2026 sobre o estado final documentado do projeto.

## Suíte Java

Comando executado:

```bash
./mvnw clean test
```

Ambiente e resultado:

- Java: OpenJDK Temurin 17.0.19;
- Maven Surefire: 3.5.6;
- classes de teste: 27;
- testes: 255;
- falhas: 0;
- erros: 0;
- ignorados: 0;
- resultado: `BUILD SUCCESS`;
- duração total da construção: 16,483 segundos.

O arquivo [surefire-resumo.csv](surefire-resumo.csv) registra, para cada classe, as quantidades e o tempo informados pelos relatórios XML produzidos pelo Surefire. Os relatórios completos podem ser regenerados em `target/surefire-reports/` pelo comando acima.

## Coleção Postman

A API foi iniciada com o perfil `dev` e a coleção foi executada sequencialmente por Newman:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
npx --yes newman run postman/raizes_do_nordeste.postman_collection.json \
  --reporters cli,junit \
  --reporter-junit-export target/newman-report.xml
```

Resultado:

- iterações: 1;
- requisições: 35;
- scripts de teste: 35;
- asserções: 55;
- falhas: 0;
- duração da coleção: 2,8 segundos;
- tempo médio de resposta: 73 milissegundos;
- contrato OpenAPI validado pelo cenário T35: 49 operações.

O arquivo [newman-report.xml](newman-report.xml) é o relatório JUnit produzido nessa execução. Ele preserva os nomes dos cenários e das asserções, os tempos observados e a ausência de falhas sem armazenar tokens JWT nem corpos das respostas.

Os tempos refletem uma execução local com H2 e dados conhecidos. Eles demonstram funcionamento nas condições descritas, mas não constituem ensaio de carga, disponibilidade ou desempenho de produção.
