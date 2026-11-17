package school.sptech.hashMap.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import school.sptech.hashMap.cache.ConteudoCache;
import school.sptech.hashMap.entity.Conteudo;
import school.sptech.hashMap.repository.ConteudoRepository;

import java.time.LocalDateTime;
import java.util.function.Consumer;

@Service
public class ConteudoService {

    private static final Logger log = LoggerFactory.getLogger(ConteudoService.class);
    private final ConteudoRepository repository;
    private final ConteudoCache cache;

    public ConteudoService(ConteudoRepository repository, ConteudoCache cache) {
        this.repository = repository;
        this.cache = cache;
    }

    public synchronized Conteudo buscar(Long id, Consumer<String> informarCache) {
        long inicio = System.nanoTime();
        String resultado = "MISS";
        try {
            Conteudo conteudo = cache.buscar(id);
            if (conteudo != null) {
                resultado = "HIT";
            } else {
                simularFonteLenta();
                conteudo = buscarNoBanco(id);
                cache.armazenar(conteudo);
            }
            informarCache.accept(resultado);
            return conteudo;
        } finally {
            long tempoMs = (System.nanoTime() - inicio) / 1_000_000;
            log.info("Busca {} ID={} tempo={} ms", resultado, id, tempoMs);
        }
    }

    public Conteudo cadastrar(Conteudo conteudo) {
        conteudo.setAtualizadoEm(LocalDateTime.now());
        return repository.save(conteudo);
    }

    public synchronized Conteudo atualizar(Long id, Conteudo novosDados) {
        Conteudo conteudo = buscarNoBanco(id);
        conteudo.setTitulo(novosDados.getTitulo());
        conteudo.setCorpo(novosDados.getCorpo());
        conteudo.setAutor(novosDados.getAutor());
        conteudo.setAtualizadoEm(LocalDateTime.now());
        Conteudo atualizado = repository.save(conteudo);
        cache.invalidar(id);
        return atualizado;
    }

    public synchronized void excluir(Long id) {
        Conteudo conteudo = buscarNoBanco(id);
        repository.delete(conteudo);
        cache.invalidar(id);
    }

    private Conteudo buscarNoBanco(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Conteúdo não encontrado."));
    }

    private void simularFonteLenta() {
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("A busca foi interrompida.", e);
        }
    }
}
