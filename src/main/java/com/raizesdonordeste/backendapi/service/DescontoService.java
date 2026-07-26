package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.model.*;
import com.raizesdonordeste.backendapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class DescontoService {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final int ESCALA_PERCENTUAL = 4;
    private static final int ESCALA_MONETARIA = 2;

    private final CampanhaRepository campanhaRepository;
    private final CupomRepository cupomRepository;
    private final CupomUsadoRepository cupomUsadoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public BigDecimal obterPrecoDoProduto(Produto produto, Long unidadeId) {
        Objects.requireNonNull(produto, "Produto nao pode ser nulo");
        Objects.requireNonNull(unidadeId, "UnidadeId nao pode ser nulo");

        LocalDateTime agora = LocalDateTime.now();

        return campanhaRepository.findCampanhaAtiva(produto.getId(), unidadeId, agora)
                .map(campanha -> {
                    BigDecimal precoPromocional = campanha.getPrecoPromocional();
                    validarPrecoPositivo(precoPromocional, "preco promocional");
                    
                    log.info("Campanha aplicada: {} | Produto: {} | Preco: {} -> {}", 
                            campanha.getNome(), produto.getNome(),
                            produto.getPrecoVigente(), precoPromocional);
                    return precoPromocional;
                })
                .orElseGet(() -> {
                    BigDecimal precoVigente = produto.getPrecoVigente();
                    validarPrecoPositivo(precoVigente, "preco vigente");
                    return precoVigente;
                });
    }

    @Transactional(readOnly = true)
    public BigDecimal validarCupom(String codigoCupom, BigDecimal subtotal, Long unidadeId) {
        if (codigoCupom == null || codigoCupom.isBlank()) {
            return BigDecimal.ZERO;
        }

        Objects.requireNonNull(subtotal, "Subtotal nao pode ser nulo");
        Objects.requireNonNull(unidadeId, "UnidadeId nao pode ser nulo");
        validarSubtotalPositivo(subtotal);

        Cupom cupom = buscarCupomPorCodigo(codigoCupom);
        validarCupomAtivo(cupom);
        validarPeriodoValidade(cupom);
        validarValorMinimo(cupom, subtotal);
        validarRestricaoUnidade(cupom, unidadeId);
        validarLimiteUsos(cupom);

        return calcularDescontoCupom(cupom, subtotal);
    }

    @Transactional
    public void registrarUsoCupom(String codigoCupom, Pedido pedido, Usuario usuario, BigDecimal valorDesconto) {
        Objects.requireNonNull(codigoCupom, "Codigo do cupom nao pode ser nulo");
        Objects.requireNonNull(pedido, "Pedido nao pode ser nulo");
        Objects.requireNonNull(usuario, "Usuario nao pode ser nulo");
        Objects.requireNonNull(valorDesconto, "Valor do desconto nao pode ser nulo");

        Cupom cupom = buscarCupomPorCodigo(codigoCupom);
        LocalDateTime agora = LocalDateTime.now();

        if (cupomRepository.reservarUsoSeDisponivel(cupom.getId()) != 1) {
            throw new RegraNegocioException("CUPOM_ESGOTADO",
                    "O cupom '" + cupom.getCodigo() + "' atingiu o limite de usos.");
        }

        CupomUsado uso = new CupomUsado();
        uso.setCupom(cupom);
        uso.setPedido(pedido);
        uso.setUsuario(usuario);
        uso.setValorDesconto(valorDesconto);
        uso.setUtilizadoEm(agora);
        cupomUsadoRepository.save(uso);

        log.info("Auditoria registrada: Cupom {} | Pedido {} | Desconto: {}", 
                cupom.getCodigo(), pedido.getId(), valorDesconto);
    }

    @Transactional
    public void estornarBeneficios(Pedido pedido) {
        Objects.requireNonNull(pedido, "Pedido nao pode ser nulo");

        int pontosUtilizados = pedido.getPontosUtilizados() != null ? pedido.getPontosUtilizados() : 0;
        int pontosConcedidos = pedido.getPontosConcedidos() != null ? pedido.getPontosConcedidos() : 0;
        if ((pontosUtilizados > 0 || pontosConcedidos > 0) && pedido.getUsuario() != null) {
            Usuario usuario = pedido.getUsuario();
            int saldoAtual = usuario.getPontos() != null ? usuario.getPontos() : 0;
            int saldoAposCompensacao = saldoAtual + pontosUtilizados - pontosConcedidos;
            usuario.setPontos(saldoAposCompensacao);
            pedido.setPontosUtilizados(0);
            pedido.setPontosConcedidos(0);
            usuarioRepository.save(usuario);
            log.info("Fidelidade revertida | Pedido: {} | Consumidos devolvidos: {} | Concedidos removidos: {} | Saldo: {}",
                    pedido.getId(), pontosUtilizados, pontosConcedidos, saldoAposCompensacao);
            if (saldoAposCompensacao < 0) {
                log.warn("Saldo devedor de fidelidade registrado | Usuario: {} | Saldo: {}",
                        usuario.getId(), saldoAposCompensacao);
            }
        }

        cupomUsadoRepository.findByPedidoAndEstornadoEmIsNull(pedido).ifPresent(uso -> {
            Cupom cupom = uso.getCupom();
            if (cupomRepository.liberarUso(cupom.getId()) != 1) {
                throw new RegraNegocioException("ESTORNO_CUPOM_INCONSISTENTE",
                        "Nao foi possivel liberar o uso do cupom do pedido " + pedido.getId() + ".");
            }
            uso.setEstornadoEm(LocalDateTime.now());
            cupomUsadoRepository.save(uso);
            log.info("Cupom estornado | Pedido: {} | Cupom: {}", pedido.getId(), cupom.getCodigo());
        });
    }

    private Cupom buscarCupomPorCodigo(String codigoCupom) {
        return cupomRepository.findByCodigo(codigoCupom.toUpperCase())
                .orElseThrow(() -> new RegraNegocioException("CUPOM_INVALIDO", 
                        "O cupom '" + codigoCupom + "' nao existe."));
    }

    private void validarCupomAtivo(Cupom cupom) {
        if (!Boolean.TRUE.equals(cupom.getAtivo())) {
            throw new RegraNegocioException("CUPOM_INATIVO", 
                    "O cupom '" + cupom.getCodigo() + "' nao esta mais ativo.");
        }
    }

    private void validarPeriodoValidade(Cupom cupom) {
        LocalDateTime agora = LocalDateTime.now();
        
        if (agora.isBefore(cupom.getDataInicio())) {
            throw new RegraNegocioException("CUPOM_FORA_PRAZO", 
                    "O cupom '" + cupom.getCodigo() + "' ainda nao esta valido.");
        }
        
        if (agora.isAfter(cupom.getDataFim())) {
            throw new RegraNegocioException("CUPOM_EXPIRADO", 
                    "O cupom '" + cupom.getCodigo() + "' expirou.");
        }
    }

    private void validarValorMinimo(Cupom cupom, BigDecimal subtotal) {
        if (subtotal.compareTo(cupom.getValorMinimoPedido()) < 0) {
            throw new RegraNegocioException("CUPOM_VALOR_MINIMO",
                    "O pedido deve ter valor minimo de R$ " + cupom.getValorMinimoPedido() + 
                    " para usar este cupom.");
        }
    }

    private void validarRestricaoUnidade(Cupom cupom, Long unidadeId) {
        if (cupom.getUnidade() != null && !cupom.getUnidade().getId().equals(unidadeId)) {
            throw new RegraNegocioException("CUPOM_UNIDADE_INVALIDA",
                    "O cupom '" + cupom.getCodigo() + "' e valido apenas na unidade " + 
                    cupom.getUnidade().getNome() + ".");
        }
    }

    private void validarLimiteUsos(Cupom cupom) {
        if (cupom.getUsoMaximo() != null && cupom.getUsoAtual() >= cupom.getUsoMaximo()) {
            throw new RegraNegocioException("CUPOM_ESGOTADO",
                    "O cupom '" + cupom.getCodigo() + "' atingiu o limite de usos.");
        }
    }

    private BigDecimal calcularDescontoCupom(Cupom cupom, BigDecimal subtotal) {
        if (cupom.getTipoDesconto() == TipoDesconto.PERCENTUAL) {
            BigDecimal percentual = cupom.getValor()
                    .divide(CEM, ESCALA_PERCENTUAL, RoundingMode.HALF_UP);
            BigDecimal desconto = subtotal.multiply(percentual)
                    .setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
            validarPrecoPositivo(desconto, "desconto");
            return desconto;
        } else {
            BigDecimal desconto = cupom.getValor().min(subtotal);
            validarPrecoPositivo(desconto, "desconto");
            return desconto;
        }
    }

    private void validarPrecoPositivo(BigDecimal valor, String campo) {
        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new RegraNegocioException("VALOR_INVALIDO",
                    "O " + campo + " nao pode ser negativo.");
        }
    }

    private void validarSubtotalPositivo(BigDecimal subtotal) {
        if (subtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new RegraNegocioException("SUBTOTAL_INVALIDO",
                    "O subtotal do pedido nao pode ser negativo.");
        }
    }
}
