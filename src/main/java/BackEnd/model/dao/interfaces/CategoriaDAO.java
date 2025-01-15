// Em src/main/java/seuprojeto/model/dao/interfaces/CategoriaDAO.java

package BackEnd.model.dao.interfaces;

import BackEnd.model.entity.Categoria;
import BackEnd.model.entity.Item;

import java.util.List;

public interface CategoriaDAO {
    List<Categoria> listarCategorias() throws Exception;
    Categoria salvarCategoria(Categoria categoria) throws Exception;
    Categoria buscarCategoriaPorNome(String nome) throws Exception;
}