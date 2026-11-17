package school.sptech.hashMap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import school.sptech.hashMap.repository.ConteudoRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ConteudoApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConteudoRepository repository;

    @Test
    void validaCadastroCacheAtualizacaoExclusaoEEstatisticas() throws Exception {
        mockMvc.perform(post("/conteudos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Primeiro","corpo":"Texto inicial","autor":"Ana"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.atualizadoEm").isNotEmpty());

        Long id = repository.findAll().getFirst().getId();
        mockMvc.perform(get("/conteudos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "MISS"))
                .andExpect(jsonPath("$.titulo").value("Primeiro"));
        mockMvc.perform(get("/conteudos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "HIT"));

        mockMvc.perform(put("/conteudos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Atualizado","corpo":"Novo texto","autor":"Ana"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(get("/conteudos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "MISS"))
                .andExpect(jsonPath("$.titulo").value("Atualizado"));
        mockMvc.perform(get("/conteudos/{id}", id))
                .andExpect(header().string("X-Cache", "HIT"));

        mockMvc.perform(delete("/conteudos/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/conteudos/{id}", id))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/conteudos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Ausente","corpo":"Texto","autor":"Ana"}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/conteudos/{id}", id))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/cache/estatisticas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanhoAtual").value(0))
                .andExpect(jsonPath("$.limite").value(5))
                .andExpect(jsonPath("$.hits").value(2))
                .andExpect(jsonPath("$.misses").value(3))
                .andExpect(jsonPath("$.evictions").value(0))
                .andExpect(jsonPath("$.hitRatio").value(0.4));

        mockMvc.perform(post("/conteudos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Segundo","corpo":"Texto","autor":"Ana"}
                                """))
                .andExpect(status().isCreated());
        Long segundoId = repository.findAll().getFirst().getId();
        mockMvc.perform(get("/conteudos/{id}", segundoId))
                .andExpect(header().string("X-Cache", "MISS"));
        mockMvc.perform(delete("/cache"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/cache/estatisticas"))
                .andExpect(jsonPath("$.tamanhoAtual").value(0))
                .andExpect(jsonPath("$.misses").value(4));
    }
}
