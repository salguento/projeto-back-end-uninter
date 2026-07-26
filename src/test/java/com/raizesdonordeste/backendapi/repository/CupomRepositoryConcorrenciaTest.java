package com.raizesdonordeste.backendapi.repository;

import com.raizesdonordeste.backendapi.model.Cupom;
import com.raizesdonordeste.backendapi.model.TipoDesconto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class CupomRepositoryConcorrenciaTest {

    @Autowired private CupomRepository cupomRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void somenteUmaTransacaoDeveConsumirOUltimoUsoDisponivel() throws Exception {
        Cupom cupom = new Cupom();
        cupom.setCodigo("CONCORRENTE1");
        cupom.setTipoDesconto(TipoDesconto.PERCENTUAL);
        cupom.setValor(new BigDecimal("10.00"));
        cupom.setValorMinimoPedido(BigDecimal.ZERO);
        cupom.setDataInicio(LocalDateTime.now().minusDays(1));
        cupom.setDataFim(LocalDateTime.now().plusDays(1));
        cupom.setUsoMaximo(1);
        cupom.setUsoAtual(0);
        cupom = cupomRepository.save(cupom);

        Long cupomId = cupom.getId();
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        try {
            var tarefa = (java.util.concurrent.Callable<Integer>) () -> {
                assertTrue(inicio.await(5, TimeUnit.SECONDS));
                return transactionTemplate.execute(status -> cupomRepository.reservarUsoSeDisponivel(cupomId));
            };
            Future<Integer> primeira = executor.submit(tarefa);
            Future<Integer> segunda = executor.submit(tarefa);
            inicio.countDown();

            List<Integer> resultados = List.of(primeira.get(5, TimeUnit.SECONDS),
                    segunda.get(5, TimeUnit.SECONDS));

            assertEquals(1, resultados.stream().mapToInt(Integer::intValue).sum());
            assertEquals(1, cupomRepository.findById(cupomId).orElseThrow().getUsoAtual());
        } finally {
            executor.shutdownNow();
        }
    }
}
