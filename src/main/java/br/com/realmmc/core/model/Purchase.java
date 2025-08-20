package br.com.realmmc.core.model;

import java.util.Date;

public record Purchase(
        String type, // "VIP" ou "CASH"
        String name, // Nome do grupo ou quantidade de Cash
        String id,   // ID da transação
        Date date,
        String status, // Ativado, Expirado, Pendente
        Date expirationDate // Apenas para VIPs
) implements Comparable<Purchase> {

    @Override
    public int compareTo(Purchase o) {
        // Ordena as compras da mais recente para a mais antiga
        return o.date().compareTo(this.date());
    }
}