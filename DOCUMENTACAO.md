# Documenta��o do Projeto RFVendas

## 1. Vis�o Geral

O RFVendas � uma aplica��o desktop para gerenciamento de vendas, desenvolvida em JavaFX, com persist�ncia em MySQL. O sistema � modularizado e segue a arquitetura MVC (Model-View-Controller), facilitando manuten��o, extensibilidade e organiza��o do c�digo.

---

## 2. Arquitetura e Estrutura de Diret�rios

```mermaid
graph TD
    Main[Main (JavaFX Application)]
    LoginController[LoginController]
    MainController[MainController]
    OutrosControllers[Outros Controllers]
    FXML[Arquivos FXML]
    CSS[styles.css]
    Imagens[icon.png, icon.ico]
    Service[Services]
    DAO[DAOs]
    Entity[Entities]
    Util[Utils]
    MySQL[(MySQL Database)]

    Main -->|start()| LoginController
    LoginController -->|Login bem-sucedido| MainController
    MainController --> OutrosControllers
    MainController --> Service
    OutrosControllers --> Service
    Service --> DAO
    DAO --> MySQL
    FXML --> Main
    CSS --> Main
    Imagens --> Main
    Util --> Main
    Entity --> DAO
    Entity --> Service
```

**Diret�rios principais:**
- `src/main/java/BackEnd/`: c�digo-fonte principal.
  - `controller/`: controllers das telas (um para cada FXML).
  - `model/entity/`: entidades de dom�nio.
  - `model/dao/interfaces/` e `model/dao/impl/`: interfaces e implementa��es DAO.
  - `model/service/`: l�gica de neg�cio.
  - `util/`: utilit�rios (conex�o, valida��o, sess�o, etc).
- `src/main/resources/`: recursos da aplica��o.
  - `fxml/`: arquivos de interface.
  - `styles/`: estilos CSS.
  - `images/`: �cones.

---

## 3. Fluxo Principal da Aplica��o

- **Inicializa��o:** Ao iniciar, carrega `Login.fxml` e aplica o tema de `styles.css`.
- **Login:** Usu�rio informa credenciais. O `LoginController` valida usando o servi�o de autentica��o e consulta o banco via DAO.
- **Permiss�es:** Ap�s login, o sistema identifica o n�vel de acesso do usu�rio (ex: administrador, operador) e ajusta as permiss�es de acordo.
- **Tela Principal:** Ap�s autentica��o, importa backup do banco (se necess�rio) e carrega `Main.fxml`.
- **Navega��o:** O usu�rio acessa funcionalidades (cadastro, listagem, pedidos, etc.) via controllers espec�ficos, que interagem com services e DAOs.

### Fluxo de Autentica��o e Permiss�es

```mermaid
sequenceDiagram
    participant U as Usu�rio
    participant LC as LoginController
    participant US as UsuarioService
    participant UD as UsuarioDAO
    participant MC as MainController

    U->>LC: Informa usu�rio e senha
    LC->>US: Solicita autentica��o
    US->>UD: Busca usu�rio no banco
    UD-->>US: Retorna dados do usu�rio
    US-->>LC: Valida senha (BCrypt) e n�vel de acesso
    LC-->>U: Login sucesso/erro
    LC->>MC: Se sucesso, carrega tela principal e define permiss�es
```

- O sistema utiliza BCrypt para hash de senha.
- O n�vel de acesso � definido pela entidade `NivelAcesso` e controla o acesso a funcionalidades.

---

## 4. Principais Entidades e Exemplos de Uso

### Cliente

```java
Cliente cliente = new Cliente("Jo�o Silva", "joao@email.com", "123.456.789-00");
clienteService.cadastrarCliente(cliente);
```

### Pedido

```java
Pedido pedido = new Pedido(cliente, LocalDate.now(), StatusPedido.ABERTO);
pedidoService.registrarPedido(pedido);
```

### Item

```java
Item item = new Item("Caneta", "Material Escolar", 2.50, 100);
itemService.cadastrarItem(item);
```

- As entidades s�o persistidas via DAOs e manipuladas via services.
- O relacionamento entre entidades (ex: Pedido e ItemPedido) � gerenciado nas camadas de servi�o e DAO.

---

## 5. Customiza��o Visual

- **Altera��o de Tema:** Edite `src/main/resources/styles/styles.css` para modificar cores, fontes e layout.
- **�cones:** Substitua ou adicione arquivos em `src/main/resources/images/` (ex: `icon.png`, `icon.ico`) para personalizar �cones da aplica��o.
- **FXML:** As telas podem ser customizadas editando os arquivos em `src/main/resources/fxml/`.

---

## 6. Depend�ncias

- JavaFX (controls, fxml, graphics, base)
- MySQL Connector/J
- BCrypt (org.mindrot.jbcrypt)
- JUnit (testes)

---

## 7. Build e Execu��o

- **Pr�-requisitos:** Java 17+, Maven.
- **Build:** 
  ```
  mvn clean package
  ```
- **Execu��o:** 
  ```
  mvn javafx:run
  ```
  ou
  ```
  java -jar target/RFVendas-1.0-SNAPSHOT.jar
  ```

---

## 8. Observa��es

- Modulariza��o via `module-info.java`.
- Estrutura extens�vel para novas telas e entidades.
- Backup do banco importado ao abrir a tela principal.
- Testes automatizados com JUnit.