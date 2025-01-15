package BackEnd.model.dao.impl;

import BackEnd.model.dao.interfaces.ClienteDAO;
import BackEnd.model.entity.Cliente;
import BackEnd.model.entity.Grupo;
import BackEnd.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAOImpl implements ClienteDAO {

    @Override
    public void criar(Cliente cliente) throws Exception {
        String sql = "INSERT INTO clientes (id, nome, cpf_cnpj, logradouro, bairro, cidade, numero, complemento, telefone_celular, email, comprador, tipo_cliente, id_grupo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, cliente.getId());
            stmt.setString(2, cliente.getNome());
            stmt.setString(3, cliente.getCpfCnpj());
            stmt.setString(4, cliente.getLogradouro());
            stmt.setString(5, cliente.getBairro());
            stmt.setString(6, cliente.getCidade());
            stmt.setString(7, cliente.getNumero());
            stmt.setString(8, cliente.getComplemento());
            stmt.setString(9, cliente.getTelefoneCelular());
            stmt.setString(10, cliente.getEmail());
            stmt.setString(11, cliente.getComprador());
            stmt.setString(12, cliente.getTipoCliente().name());
            if (cliente.getGrupo() != null) {
                stmt.setInt(13, cliente.getGrupo().getId());
            } else {
                stmt.setNull(13, java.sql.Types.INTEGER);
            }
            stmt.executeUpdate();
        }
    }

    @Override
    public Cliente buscarPorId(String id) throws Exception {
        String sql = "SELECT c.*, g.id as grupo_id, g.nome as grupo_nome FROM clientes c LEFT JOIN grupos g ON c.id_grupo = g.id WHERE c.id = ?";
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearCliente(rs);
                }
            }
        }
        return null;
    }

    @Override
    public Cliente buscarPorCPFCNPJ(String cpfCnpj) throws Exception {
        String sql = "SELECT c.*, g.id as grupo_id, g.nome as grupo_nome FROM clientes c LEFT JOIN grupos g ON c.id_grupo = g.id WHERE c.cpf_cnpj = ?";
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, cpfCnpj);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearCliente(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Cliente> listarTodos() throws Exception {
        String sql = "SELECT c.*, g.id as grupo_id, g.nome as grupo_nome FROM clientes c LEFT JOIN grupos g ON c.id_grupo = g.id ORDER BY c.nome";
        List<Cliente> clientes = new ArrayList<>();
        try (Connection conexao = ConnectionFactory.getConnection();
             Statement stmt = conexao.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                clientes.add(mapearCliente(rs));
            }
        }
        return clientes;
    }

    @Override
    public void atualizar(Cliente cliente) throws Exception {
        String sql = "UPDATE clientes SET nome = ?, cpf_cnpj = ?, logradouro = ?, bairro = ?, cidade = ?, numero = ?, complemento = ?, telefone_celular = ?, email = ?, comprador = ?, tipo_cliente = ?, id_grupo = ? WHERE id = ?";
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, cliente.getNome());
            stmt.setString(2, cliente.getCpfCnpj());
            stmt.setString(3, cliente.getLogradouro());
            stmt.setString(4, cliente.getBairro());
            stmt.setString(5, cliente.getCidade());
            stmt.setString(6, cliente.getNumero());
            stmt.setString(7, cliente.getComplemento());
            stmt.setString(8, cliente.getTelefoneCelular());
            stmt.setString(9, cliente.getEmail());
            stmt.setString(10, cliente.getComprador());
            stmt.setString(11, cliente.getTipoCliente().name());
            if (cliente.getGrupo() != null) {
                stmt.setInt(12, cliente.getGrupo().getId());
            } else {
                stmt.setNull(12, java.sql.Types.INTEGER);
            }
            stmt.setString(13, cliente.getId());
            stmt.executeUpdate();
        }
    }

    @Override
    public void deletar(String id) throws Exception {
        String sql = "DELETE FROM clientes WHERE id = ?"; // Mantenha o ? para prepared statement
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, id); // Correto: setString para coluna VARCHAR
            stmt.executeUpdate();
        }
    }

    private Cliente mapearCliente(ResultSet rs) throws Exception {
        Cliente cliente = new Cliente();
        cliente.setId(rs.getString("id"));
        cliente.setNome(rs.getString("nome"));
        cliente.setCpfCnpj(rs.getString("cpf_cnpj"));
        cliente.setLogradouro(rs.getString("logradouro"));
        cliente.setBairro(rs.getString("bairro"));
        cliente.setCidade(rs.getString("cidade"));
        cliente.setNumero(rs.getString("numero"));
        cliente.setComplemento(rs.getString("complemento"));
        cliente.setTelefoneCelular(rs.getString("telefone_celular"));
        cliente.setEmail(rs.getString("email"));
        cliente.setComprador(rs.getString("comprador"));
        cliente.setTipoCliente(Cliente.TipoCliente.valueOf(rs.getString("tipo_cliente")));

        // Trata o relacionamento com Grupo
        int grupoId = rs.getInt("grupo_id");
        if (grupoId != 0) {
            Grupo grupo = new Grupo();
            grupo.setId(grupoId);
            grupo.setNome(rs.getString("grupo_nome"));
            cliente.setGrupo(grupo);
        }

        return cliente;
    }
}