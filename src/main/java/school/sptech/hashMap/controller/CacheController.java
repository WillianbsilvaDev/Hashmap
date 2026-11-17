package school.sptech.hashMap.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import school.sptech.hashMap.cache.ConteudoCache;
import school.sptech.hashMap.dto.EstatisticasCacheDto;

@RestController
@RequestMapping("/cache")
public class CacheController {

    private final ConteudoCache cache;

    public CacheController(ConteudoCache cache) {
        this.cache = cache;
    }

    @GetMapping("/estatisticas")
    public EstatisticasCacheDto estatisticas() {
        synchronized (cache) {
            return new EstatisticasCacheDto(cache.getTamanhoAtual(), cache.getLimite(),
                    cache.getHits(), cache.getMisses(), cache.getEvictions(), cache.getHitRatio());
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> limpar() {
        cache.limpar();
        return ResponseEntity.noContent().build();
    }
}
