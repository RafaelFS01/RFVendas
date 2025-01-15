// Em src/main/java/seuprojeto/model/dao/interfaces/ItemDAO.java

package BackEnd.model.dao.interfaces;

import BackEnd.model.entity.Item;
import java.util.List;

public interface ItemDAO {
    void salvarItem(Item item) throws Exception;
    void atualizar(Item item) throws Exception;
    boolean buscarItemPorNome(String nome) throws Exception;
    Item buscarItemPorId(int id) throws Exception;
    List<Item> listarItens() throws Exception;
    List<Item> listarItensPorCategoria(int idCategoria) throws Exception;
    List<Item> listarItensAbaixoDoMinimo() throws Exception;
    void deletar(int id) throws Exception;
}