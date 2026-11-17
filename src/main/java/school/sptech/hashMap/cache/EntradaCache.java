package school.sptech.hashMap.cache;

import school.sptech.hashMap.entity.Conteudo;

import java.time.Instant;

record EntradaCache(Conteudo conteudo, Instant armazenadoEm, Instant ultimoAcesso,
                    long ordemAcesso) {
}
