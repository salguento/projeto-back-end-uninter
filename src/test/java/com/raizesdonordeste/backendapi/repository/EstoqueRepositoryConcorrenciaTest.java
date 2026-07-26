package com.raizesdonordeste.backendapi.repository;

import com.raizesdonordeste.backendapi.model.Estoque;
import com.raizesdonordeste.backendapi.model.Produto;
import com.raizesdonordeste.backendapi.model.Unidade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EstoqueRepositoryConcorrenciaTest {

    @Autowired private EstoqueRepository estoqueRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private UnidadeRepository unidadeRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void somenteUmaReservaDeveConsumirAUltimaUnidade() throws Exception {
        Unidade unidade = new Unidade();
        unidade.setNome("Unidade Concorrencia");
        unidade.setEndereco("Rua Teste, 1");
        unidade.setCidade("Recife");
        unidade.setEstado("PE");
        unidade.setCep("50000000");
        unidade.setTelefone("81999999999");
        unidade = unidadeRepository.save(unidade);

        Produto produto = new Produto();
        produto.setNome("Produto Concorrencia");
        produto.setPrecoVigente(new BigDecimal("10.00"));
        produto = produtoRepository.save(produto);

        Estoque estoque = new Estoque();
        estoque.setUnidade(unidade);
        estoque.setProduto(produto);
        estoque.setQuantidade(1);
        estoqueRepository.save(estoque);

        Long unidadeId = unidade.getId();
        Long produtoId = produto.getId();
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        try {
            var tarefa = (java.util.concurrent.Callable<Integer>) () -> {
                assertTrue(inicio.await(5, TimeUnit.SECONDS));
                return transactionTemplate.execute(status ->
                        estoqueRepository.reservarEstoqueSeDisponivel(unidadeId, produtoId, 1));
            };
            Future<Integer> primeira = executor.submit(tarefa);
            Future<Integer> segunda = executor.submit(tarefa);
            inicio.countDown();

            List<Integer> resultados = List.of(primeira.get(5, TimeUnit.SECONDS),
                    segunda.get(5, TimeUnit.SECONDS));

            assertEquals(1, resultados.stream().mapToInt(Integer::intValue).sum());
            assertEquals(0, estoqueRepository.findByUnidadeIdAndProdutoId(unidadeId, produtoId)
                    .orElseThrow().getQuantidade());
        } finally {
            executor.shutdownNow();
        }
    }
}
