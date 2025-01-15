CRIAÇÃO DE TABELAS MYSQL:

create database RFVendas;
use RFVendas;

CREATE TABLE categorias (
id INT AUTO_INCREMENT PRIMARY KEY,
nome VARCHAR(255) NOT NULL UNIQUE,
descricao TEXT
);

CREATE TABLE itens (
id INT PRIMARY KEY,
nome VARCHAR(255) NOT NULL UNIQUE,
descricao TEXT,
preco_venda DECIMAL(10, 2) NOT NULL,
preco_custo DECIMAL(10, 2) NOT NULL,
unidade_medida VARCHAR(50) NOT NULL,
quantidade_estoque DECIMAL(10, 5)  NOT NULL,
quantidade_minima DECIMAL(10, 5) NOT NULL,
quantidade_atual DECIMAL(10, 5) NOT NULL DEFAULT 0,
categoria_id INT,
FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);

CREATE TABLE dependencias (
id INT PRIMARY KEY AUTO_INCREMENT,
id_item_dependente INT NOT NULL,
id_item_necessario INT NOT NULL,
id_categoria INT NOT NULL,
quantidade DECIMAL(10, 2) NOT NULL,
FOREIGN KEY (id_item_dependente) REFERENCES itens(id),
FOREIGN KEY (id_item_necessario) REFERENCES itens(id),
FOREIGN KEY (id_categoria) REFERENCES categorias(id),
CONSTRAINT check_quantidade CHECK (quantidade > 0)
);

CREATE TABLE grupos (
id INT NOT NULL AUTO_INCREMENT,
nome VARCHAR(255) NOT NULL,
PRIMARY KEY (id),
UNIQUE KEY uk_nome_grupo (nome)
);

CREATE TABLE clientes (
id VARCHAR(255) PRIMARY KEY NOT NULL,
nome VARCHAR(255) NOT NULL,
cpf_cnpj VARCHAR(14) NOT NULL,
logradouro VARCHAR(255) NOT NULL,
bairro VARCHAR(255) NOT NULL,
cidade VARCHAR(255) NOT NULL,
numero VARCHAR(20) NOT NULL,
complemento VARCHAR(255) DEFAULT NULL,
telefone_celular VARCHAR(20) NOT NULL,
email VARCHAR(255) NOT NULL,
comprador VARCHAR(255) DEFAULT NULL,
tipo_cliente ENUM('PESSOA_FISICA', 'PESSOA_JURIDICA') NOT NULL,
id_grupo INT DEFAULT NULL,
UNIQUE KEY uk_cpf_cnpj (cpf_cnpj),
FOREIGN KEY (id_grupo) REFERENCES grupos (id)
);

CREATE TABLE pedidos (
id INT AUTO_INCREMENT PRIMARY KEY,
cliente_id VARCHAR(255) NOT NULL, -- Assumindo que o ID do cliente é uma string (CPF/CNPJ)
tipo_venda ENUM('FIADO', 'VENDA', 'PEDIDO') NOT NULL,
data_pedido DATE NOT NULL,
valor_total DECIMAL(10, 2) NOT NULL, -- DECIMAL para armazenar valores monetários com precisão
status ENUM('CONCLUIDO', 'EM_ANDAMENTO', 'CANCELADO') NOT NULL,
observacoes TEXT,
FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE -- Se um cliente for deletado, seus pedidos também serão
);

CREATE TABLE itens_pedido (
id INT AUTO_INCREMENT PRIMARY KEY,
pedido_id INT NOT NULL,
item_id INT NOT NULL,
quantidade DECIMAL(10, 2) NOT NULL, -- Alterado para DECIMAL para maior precisão
preco_venda DECIMAL(10, 2) NOT NULL, -- DECIMAL para valores monetários
FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE, -- Se um pedido for deletado, seus itens também serão
FOREIGN KEY (item_id) REFERENCES itens(id) -- Mantém a integridade referencial, mas você pode ajustar o comportamento ON DELETE conforme necessário
);

CREATE TABLE usuarios (
id INT PRIMARY KEY AUTO_INCREMENT,
username VARCHAR(50) UNIQUE NOT NULL,
password VARCHAR(255) NOT NULL,
nome VARCHAR(100) NOT NULL,
email VARCHAR(100) UNIQUE NOT NULL,
nivel_acesso INT NOT NULL,
ativo BOOLEAN DEFAULT TRUE,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para otimização de performance
CREATE INDEX idx_usuarios_username ON usuarios(username);
CREATE INDEX idx_clientes_nome ON clientes(nome);

-- Dados iniciais
INSERT INTO usuarios (username, password, nome, email, nivel_acesso)
VALUES ('admin', '1234', 'Administrador', 'admin@BackEnd.com', 1);