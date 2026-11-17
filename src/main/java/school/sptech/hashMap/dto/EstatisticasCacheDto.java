package school.sptech.hashMap.dto;

public record EstatisticasCacheDto(int tamanhoAtual, int limite, long hits, long misses,
                                  long evictions, double hitRatio) {
}
