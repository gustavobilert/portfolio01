# Projeto de Gustavo Bilert

Projeto desenvolvido como solução ao desafio técnico para o processo seletivo de uma empresa.

A proposta era fazer uma api de votação, onde, após cadastrar uma pauta, a mesma deveria ser iniciada 
e ficar aberta para votação por um tempo determinado.
Como atividades bonus, foi proposto consumir uma API externa, publicar o resultado através de mensageria, 
além de realizar testes de performance e definir uma estratégia de versionamento da API.

Este projeto utiliza o framework Quarkus, uma iniciativa da Red Hat que visa prover uma melhor performance
para aplicações Java, minimizando o tempo de inicialização, tamanho do deploy e o consumo de memória.

## Executando a aplicação em modo desenvolvimento

Execute o seguinte comando na raiz do projeto, isso já habilita o live-reload:
```shell script
./mvnw compile quarkus:dev
```

## Possíveis problemas

```
invalid source release: 11
```

Se este problema aparecer, verifique se seu JDK está atualizado. Foi testado com OpenJDK na última versão, em ambiente Windows.

Se o problema aparecer ao executar os testes, verifique se a IDE está pegando o SDK correto.

No IntelliJ foi necessário ir em Project Structure... -> Project e alterar o Project SDK de 1.8 para 15

Se o teste `testCreateWithoutBody()` falhar devido ao nome do campo (arg0 em vez de poll) é provável que seja o mesmo problema,
 neste caso é necessário alterar a JDK utilizada no test runner. 

## Documentação

A documentação da API pode ser acessada através do Swagger UI (http://localhost:8080/swagger-ui) ou de forma textual através da Openapi (http://localhost:8080/openapi)

Na pasta src/test/postman está disponível uma collection do Postman com a sequência básica de utilização da API.
O objetivo é servir como exemplo para facilitar a familiarização com a API.

O código foi escrito e documentado em inglês para facilitar a interação com times globais.

## Mensageria

Para publicar o resultado da votação através de mensageria, é necessário executar o ActiveMQ Artemis.
Você pode instalar e rodar ele localmente, ou através do docker, executando o seguinte comando na pasta `docker`:
```shell script
docker-compose up
```
É necessário habilitar a mensageria no arquivo application.properties, basta `comentar` ou alterar para `true` estas duas linhas:
```properties
mp.messaging.outgoing.poll-create.enabled=false
mp.messaging.incoming.polls.enabled=false
```
Depois basta iniciar a votação de uma pauta, ao terminar a duração da mesma, uma mensagem com o resultado será publicada no ActiveMQ, no tópico 'polls'.
A mensagem será consumida pela própria aplicação e mostrada no log.
Também é possível verificar pelo console do ActiveMQ (http://localhost:8161/console), utilizando `login = quarkus` e `senha = quarkus` 

## Teste de performance

Está disponível uma collection do postman com teste de performance, ela dispara um número configurável de requisições (na aba Tests da request `Place vote`)
 e mede o 90-percentil dos tempos de resposta.
Para executá-lo basta deixar a aplicação rodando, importar a collection no Postman e executá-la com o Collection Runner (executar as requests manualmente em ordem também funciona).

## Versionamento

Para o versionamento da API, não é interessante colocar a versão na URI, pois isso forçaria os consumidores a atualizarem todas as chamadas sempre que a API fosse atualizada.
Então utilizamos o versionamento através do header "Accept", desta forma o consumidor da API pode solicitar que os dados sejam retornados pela API conforme uma versão específica.
Também pode solicitar simplesmente "application/json", neste caso a aplicação retorna a última versão, possibilitando atualização automática quando desejado.
A versão na API só muda quando houver quebra de compatibilidade entre as versões, o que deve ser evitado o máximo possível, de modo a minimizar o impacto sobre os consumidores da API.

## Problemas conhecidos

- Falta validar a ordem das chamadas, para prevenir, por exemplo, que sejam registrados votos em uma pauta que ainda não foi iniciada.

