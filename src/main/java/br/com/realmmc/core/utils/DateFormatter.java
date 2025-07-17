package br.com.realmmc.core.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Classe utilitária para formatar datas em um padrão consistente.
 */
public final class DateFormatter {

    private static final SimpleDateFormat DATE_FORMAT;

    // Bloco estático para inicializar o formatador uma única vez, melhorando a performance.
    static {
        DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm");
        // Define o fuso horário para garantir consistência em diferentes máquinas
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));
    }

    private DateFormatter() {
        // Construtor privado para impedir a instanciação de classes utilitárias
    }

    /**
     * Formata um objeto Date para uma string legível.
     * @param date O objeto Date a ser formatado.
     * @return Uma string formatada (ex: "04/07/2025 às 11:50") ou uma string vazia se a data for nula.
     */
    public static String format(Date date) {
        if (date == null) {
            return ""; // Retorna vazio em vez de "N/A" para ser usado em mensagens customizáveis
        }
        return DATE_FORMAT.format(date);
    }
}