// Em src/main/java/seuprojeto/service/CompraService.java

package BackEnd.model.service;

import BackEnd.model.entity.Categoria;
import BackEnd.model.entity.Dependencia;
import BackEnd.model.entity.Item;
import BackEnd.model.entity.SugestaoCompra;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CompraService {

    private ItemService itemService;
    private DependenciaService dependenciaService;

    public CompraService() {
        this.itemService = new ItemService();
        this.dependenciaService = new DependenciaService();
    }

    public List<SugestaoCompra> calcularSugestoesCompra(Categoria categoria) throws Exception {
        List<SugestaoCompra> sugestoesCompra = new ArrayList<>();

        // 1. Obter todos os itens que tenham a quantidadeAtual menor que a quantidade mínima
        List<Item> itensAbaixoDoMinimo = itemService.listarItens().stream()
                .filter(item -> item.getQuantidadeAtual() < item.getQuantidadeMinima())
                .collect(Collectors.toList());

        // 2. Iterar sobre a lista de itens filtrados
        for (Item item : itensAbaixoDoMinimo) {
            double quantidadeNegativa = item.getQuantidadeAtual() < 0 ? item.getQuantidadeAtual() * -1 : 1;

            // 3. Obter as dependências do item
            List<Dependencia> dependencias = dependenciaService.buscarPorIdItemDependente(item.getId());

            // Filtrar as dependências pela categoria selecionada (se categoria não for nula)
            if (categoria != null) {
                dependencias = dependencias.stream()
                        .filter(dependencia -> {
                            try {
                                Item itemNecessario = itemService.buscarItemPorId(dependencia.getIdItemNecessario());
                                return itemNecessario.getCategoria().getId().equals(categoria.getId());
                            } catch (Exception e) {
                                System.err.println("Erro ao buscar item da dependência: " + e.getMessage());
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
            }

            // 4. Iterar sobre a lista de dependências
            for (Dependencia dependencia : dependencias) {
                Item itemDependencia = itemService.buscarItemPorId(dependencia.getIdItemNecessario());

                // 5. Calcular quantidadeBase, quantidadeSomada e quantidadeSugerida
                double quantidadeBase = dependencia.getQuantidade() * quantidadeNegativa;
                double quantidadeSomada = quantidadeBase + itemDependencia.getQuantidadeMinima();
                double quantidadeSugerida = quantidadeSomada - itemDependencia.getQuantidadeAtual();

                // 6. Se quantidadeSugerida for maior que zero, adicionar à lista de sugestões
                if (quantidadeSugerida > 0) {
                    sugestoesCompra.add(new SugestaoCompra(itemDependencia, quantidadeSugerida));
                }
            }
        }

        return sugestoesCompra;
    }
}