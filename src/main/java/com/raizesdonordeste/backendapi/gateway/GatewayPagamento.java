package com.raizesdonordeste.backendapi.gateway;

import com.raizesdonordeste.backendapi.model.FormaPagamento;

public interface GatewayPagamento {

    ResultadoGatewayPagamento processar(FormaPagamento formaPagamento);

    ResultadoGatewayEstorno estornar(String codigoTransacao);
}
