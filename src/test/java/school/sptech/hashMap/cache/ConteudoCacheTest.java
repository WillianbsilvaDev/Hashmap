package school.sptech.hashMap.cache;

import org.junit.jupiter.api.Test;
import school.sptech.hashMap.entity.Conteudo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class ConteudoCacheTest {

    @Test
    void removeOMenosRecenteAoAtingirOLimite() {
        ConteudoCache cache = new ConteudoCache(5, 60);
        for (long id = 1; id <= 5; id++) {
            cache.armazenar(conteudo(id));
        }
        assertNotNull(cache.buscar(1L));
        cache.armazenar(conteudo(6L));

        assertEquals(5, cache.getTamanhoAtual());
        assertEquals(1, cache.getEvictions());
        assertNull(cache.buscar(2L));
        assertNotNull(cache.buscar(1L));
        assertNotNull(cache.buscar(6L));
    }

    @Test
    void hitNaoRenovaOTtlEItemExpiradoContaComoMiss() throws InterruptedException {
        ConteudoCache cache = new ConteudoCache(5, 1);
        cache.armazenar(conteudo(1L));
        Thread.sleep(600);
        assertNotNull(cache.buscar(1L));
        Thread.sleep(500);

        assertNull(cache.buscar(1L));
        assertEquals(1, cache.getHits());
        assertEquals(1, cache.getMisses());
        assertEquals(0.5, cache.getHitRatio());
        assertEquals(0, cache.getTamanhoAtual());
        assertEquals(0, cache.getEvictions());
    }

    @Test
    void reutilizaEspacoExpiradoSemEviction() throws InterruptedException {
        ConteudoCache cache = new ConteudoCache(1, 1);
        cache.armazenar(conteudo(1L));
        Thread.sleep(1_100);
        cache.armazenar(conteudo(2L));

        assertEquals(1, cache.getTamanhoAtual());
        assertNotNull(cache.buscar(2L));
        assertEquals(0, cache.getEvictions());
    }

    @Test
    void substituirMesmoIdNaoRemoveOutroItem() {
        ConteudoCache cache = new ConteudoCache(1, 60);
        cache.armazenar(conteudo(1L));
        Conteudo atualizado = conteudo(1L);
        atualizado.setTitulo("Atualizado");
        cache.armazenar(atualizado);

        assertEquals("Atualizado", cache.buscar(1L).getTitulo());
        assertEquals(0, cache.getEvictions());
    }

    @Test
    void invalidacaoELimpezaPreservamContadores() {
        ConteudoCache cache = new ConteudoCache(5, 60);
        assertEquals(0.0, cache.getHitRatio());
        cache.armazenar(conteudo(1L));
        cache.invalidar(1L);
        assertNull(cache.buscar(1L));
        cache.armazenar(conteudo(2L));
        cache.limpar();

        assertEquals(0, cache.getTamanhoAtual());
        assertEquals(1, cache.getMisses());
        assertEquals(0, cache.getEvictions());
    }

    @Test
    void insercoesSimultaneasRespeitamLimiteEContadores() throws Exception {
        ConteudoCache cache = new ConteudoCache(5, 60);
        List<Callable<Void>> tarefas = new ArrayList<>();
        for (long id = 1; id <= 100; id++) {
            long idAtual = id;
            tarefas.add(() -> {
                cache.armazenar(conteudo(idAtual));
                cache.buscar(idAtual);
                assertTrue(cache.getTamanhoAtual() <= 5);
                return null;
            });
        }
        try (var executor = Executors.newFixedThreadPool(12)) {
            for (var resultado : executor.invokeAll(tarefas)) {
                resultado.get();
            }
        }

        assertEquals(5, cache.getTamanhoAtual());
        assertEquals(95, cache.getEvictions());
        assertEquals(100, cache.getHits() + cache.getMisses());
    }

    private Conteudo conteudo(long id) {
        Conteudo conteudo = new Conteudo();
        conteudo.setId(id);
        conteudo.setTitulo("Conteúdo " + id);
        return conteudo;
    }
}
