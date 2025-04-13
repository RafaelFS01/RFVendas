package BackEnd.model.service;

import BackEnd.model.dao.impl.ClienteDAOImpl;
import BackEnd.model.dao.interfaces.ClienteDAO;
import BackEnd.model.dao.impl.GrupoDAOImpl;
import BackEnd.model.dao.interfaces.GrupoDAO;
import BackEnd.model.entity.Cliente;
import BackEnd.model.entity.Grupo;
import BackEnd.util.ValidationHelper;

import java.util.List;

public class ClienteService {
    private final ClienteDAO clienteDAO;
    private final GrupoDAO grupoDAO;

    public ClienteService() {
        this.clienteDAO = new ClienteDAOImpl();
        this.grupoDAO = new GrupoDAOImpl();
    }

    public void cadastrarCliente(Cliente cliente) throws Exception {
        validarCliente(cliente);

        if (existePorId(cliente.getId())) {
            throw new IllegalArgumentException("Já existe um cliente com este ID.");
        }

        if (existePorCPFCNPJ(cliente.getCpfCnpj())) {
            throw new IllegalArgumentException("Já existe um cliente com este CPF/CNPJ.");
        }

        // Verifica se o grupo informado existe, caso não seja nulo
        if (cliente.getGrupo() != null && !grupoDAO.existePorId(cliente.getGrupo().getId())) {
            throw new IllegalArgumentException("O grupo informado não existe.");
        }

        try {
            clienteDAO.criar(cliente);
        } catch (Exception e) {
            throw new Exception("Erro ao cadastrar cliente: " + e.getMessage());
        }
    }

    public List<Cliente> listarTodos() throws Exception {
        try {
            return clienteDAO.listarTodos();
        } catch (Exception e) {
            throw new Exception("Erro ao listar clientes: " + e.getMessage());
        }
    }

    public boolean existePorId(String id) throws Exception {
        try {
            return clienteDAO.buscarPorId(id) != null;
        } catch (Exception e) {
            throw new Exception("Erro ao verificar existência do cliente: " + e.getMessage());
        }
    }

    public boolean existePorCPFCNPJ(String cpfCnpj) throws Exception {
        try {
            return clienteDAO.buscarPorCPFCNPJ(cpfCnpj) != null;
        } catch (Exception e) {
            throw new Exception("Erro ao verificar existência do cliente por CPF/CNPJ: " + e.getMessage());
        }
    }

    private void validarCliente(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente não pode ser nulo.");
        }
        if (ValidationHelper.isNullOrEmpty(cliente.getId())) {
            throw new IllegalArgumentException("ID do cliente é obrigatório.");
        }
        if (ValidationHelper.isNullOrEmpty(cliente.getNome())) {
            throw new IllegalArgumentException("Nome do cliente é obrigatório.");
        }
        if (cliente.getTipoCliente() == null) {
            throw new IllegalArgumentException("Tipo de cliente é obrigatório.");
        }
        if (ValidationHelper.isNullOrEmpty(cliente.getCpfCnpj())) {
            throw new IllegalArgumentException("CPF/CNPJ do cliente é obrigatório.");
        }
        if (cliente.getTipoCliente() == Cliente.TipoCliente.PESSOA_FISICA && !ValidationHelper.isValidCPF(cliente.getCpfCnpj())) {
            throw new IllegalArgumentException("CPF inválido.");
        }
        if (cliente.getTipoCliente() == Cliente.TipoCliente.PESSOA_JURIDICA && !ValidationHelper.isValidCNPJ(cliente.getCpfCnpj())) {
            throw new IllegalArgumentException("CNPJ inválido.");
        }
        if (ValidationHelper.isNullOrEmpty(cliente.getLogradouro())) {
            throw new IllegalArgumentException("Logradouro é obrigatório.");
        }
        if (ValidationHelper.isNullOrEmpty(cliente.getBairro())) {
            throw new IllegalArgumentException("Bairro é obrigatório.");
        }
        if (ValidationHelper.isNullOrEmpty(cliente.getCidade())) {
            throw new IllegalArgumentException("Cidade é obrigatória.");
        }
        if (ValidationHelper.isNullOrEmpty(cliente.getNumero())) {
            throw new IllegalArgumentException("Número é obrigatório.");
        }
        if (ValidationHelper.isNullOrEmpty(cliente.getTelefoneCelular())) {
            throw new IllegalArgumentException("Telefone celular é obrigatório.");
        }
        if (ValidationHelper.isNullOrEmpty(cliente.getEmail())) {
            throw new IllegalArgumentException("Email é obrigatório.");
        }
        if (!ValidationHelper.isValidEmail(cliente.getEmail())) {
            throw new IllegalArgumentException("Email inválido.");
        }
    }

    // Métodos para Grupo (serão movidos para GrupoService depois)
    public List<Grupo> listarGrupos() throws Exception {
        try {
            return grupoDAO.listarTodos();
        } catch (Exception e) {
            throw new Exception("Erro ao listar grupos: " + e.getMessage());
        }
    }

    public void cadastrarGrupo(String nomeGrupo) throws Exception {
        if (ValidationHelper.isNullOrEmpty(nomeGrupo)) {
            throw new IllegalArgumentException("Nome do grupo não pode ser vazio.");
        }

        try {
            Grupo grupo = new Grupo();
            grupo.setNome(nomeGrupo);
            grupoDAO.criar(grupo);
        } catch (Exception e) {
            throw new Exception("Erro ao cadastrar grupo: " + e.getMessage());
        }
    }

    public void deletar(String id) throws Exception{
        clienteDAO.deletar(id);
    }

    // Em ClienteService.java
    public Cliente buscarPorId(String id) throws Exception {
        try {
            return clienteDAO.buscarPorId(id);
        } catch (Exception e) {
            throw new Exception("Erro ao buscar cliente por ID: " + e.getMessage(), e);
        }
    }

    /**
     * Atualiza os dados de um cliente existente.
     * @param cliente O Cliente com os dados atualizados.
     * @throws Exception Se ocorrer erro de validação ou no DAO.
     */
    public void atualizarCliente(Cliente cliente) throws Exception {
        // Realiza validações básicas (as mesmas do cadastro por enquanto)
        validarCliente(cliente);

        // Validação específica de atualização: CPF/CNPJ (não pode mudar, ou se mudar não pode colidir com outro)
        // Como desabilitamos no controller, não precisamos validar colisão aqui.

        // Verifica se o cliente realmente existe para atualizar
        if (!existePorId(cliente.getId())) {
            throw new Exception("Cliente com ID " + cliente.getId() + " não encontrado para atualização.");
        }

        // Verifica se o grupo informado existe, caso não seja nulo
        if (cliente.getGrupo() != null && !grupoDAO.existePorId(cliente.getGrupo().getId())) {
            throw new IllegalArgumentException("O grupo informado não existe.");
        }

        try {
            clienteDAO.atualizar(cliente); // Chama o DAO
        } catch (Exception e) {
            throw new Exception("Erro ao atualizar cliente: " + e.getMessage(), e);
        }
    }
}