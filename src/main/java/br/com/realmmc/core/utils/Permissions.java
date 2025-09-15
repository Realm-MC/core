package br.com.realmmc.core.utils;

/**
 * Centraliza todas as permissões utilizadas nos plugins da rede RealmMC.
 * Usar esta classe evita erros de digitação ("magic strings") e facilita a manutenção.
 */
public final class Permissions {

    private Permissions() {}

    /**
     * Permissão para membros VIP (Campeão ou superior) em funcionalidades do Core/Lobby.
     */
    public static final String CORE_CHAMPION = "core.champion";

    /**
     * Permissão para membros VIP (Campeão ou superior) em funcionalidades do Rankup.
     */
    public static final String RANKUP_CHAMPION = "rankup.champion";

}