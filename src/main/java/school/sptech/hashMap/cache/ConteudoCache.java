package school.sptech.hashMap.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import school.sptech.hashMap.entity.Conteudo;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConteudoCache {

    private final Map<Long, EntradaCache> entradas = new ConcurrentHashMap<>();
    private final int limite;
    private final Duration ttl;
    private long hits;
    private long misses;
    private long evictions;
    private long sequenciaAcesso;

    public ConteudoCache(@Value("${app.cache.max-itens}") int limite,
                         @Value("${app.cache.ttl-segundos}") long ttlSegundos) {
        if (limite <= 0 || ttlSegundos <= 0) {
            throw new IllegalArgumentException("O limite e o TTL devem ser positivos.");
        }
        this.limite = limite;
        this.ttl = Duration.ofSeconds(ttlSegundos);
    }

    public synchronized Conteudo buscar(Long id) {
        Instant agora = Instant.now();
        EntradaCache entrada = entradas.get(id);
        if (entrada == null || expirou(entrada, agora)) {
            entradas.remove(id);
            misses++;
            return null;
        }
        hits++;
        entradas.put(id, new EntradaCache(entrada.conteudo(), entrada.armazenadoEm(),
                agora, ++sequenciaAcesso));
        return entrada.conteudo();
    }

    public synchronized void armazenar(Conteudo conteudo) {
        Instant agora = Instant.now();
        removerExpirados(agora);
        if (!entradas.containsKey(conteudo.getId()) && entradas.size() >= limite) {
            Long idMenosRecente = entradas.entrySet().stream()
                    .min(Comparator.comparingLong(item -> item.getValue().ordemAcesso()))
                    .orElseThrow().getKey();
            entradas.remove(idMenosRecente);
            evictions++;
        }
        entradas.put(conteudo.getId(), new EntradaCache(conteudo, agora, agora, ++sequenciaAcesso));
    }

    public synchronized void invalidar(Long id) {
        entradas.remove(id);
    }

    public synchronized void limpar() {
        entradas.clear();
    }

    public synchronized int getTamanhoAtual() {
        removerExpirados(Instant.now());
        return entradas.size();
    }

    public int getLimite() {
        return limite;
    }

    public synchronized long getHits() {
        return hits;
    }

    public synchronized long getMisses() {
        return misses;
    }

    public synchronized long getEvictions() {
        return evictions;
    }

    public synchronized double getHitRatio() {
        long buscas = hits + misses;
        return buscas == 0 ? 0.0 : (double) hits / buscas;
    }

    private boolean expirou(EntradaCache entrada, Instant agora) {
        return !agora.isBefore(entrada.armazenadoEm().plus(ttl));
    }

    private void removerExpirados(Instant agora) {
        entradas.entrySet().removeIf(item -> expirou(item.getValue(), agora));
    }
}
