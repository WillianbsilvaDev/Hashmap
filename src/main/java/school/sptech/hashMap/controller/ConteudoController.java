package school.sptech.hashMap.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import school.sptech.hashMap.dto.ConteudoRequisicaoDto;
import school.sptech.hashMap.dto.ConteudoRespostaDto;
import school.sptech.hashMap.entity.Conteudo;
import school.sptech.hashMap.mapper.ConteudoMapper;
import school.sptech.hashMap.service.ConteudoService;

import java.net.URI;

@RestController
@RequestMapping("/conteudos")
public class ConteudoController {

    private final ConteudoService service;

    public ConteudoController(ConteudoService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConteudoRespostaDto> buscar(@PathVariable Long id) {
        ResponseEntity.BodyBuilder resposta = ResponseEntity.ok();
        Conteudo conteudo = service.buscar(id, resultado -> resposta.header("X-Cache", resultado));
        return resposta.body(ConteudoMapper.paraDto(conteudo));
    }

    @PostMapping
    public ResponseEntity<ConteudoRespostaDto> cadastrar(@RequestBody ConteudoRequisicaoDto dto) {
        Conteudo conteudo = service.cadastrar(ConteudoMapper.paraEntidade(dto));
        return ResponseEntity.created(URI.create("/conteudos/" + conteudo.getId()))
                .body(ConteudoMapper.paraDto(conteudo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConteudoRespostaDto> atualizar(@PathVariable Long id,
                                                       @RequestBody ConteudoRequisicaoDto dto) {
        Conteudo conteudo = service.atualizar(id, ConteudoMapper.paraEntidade(dto));
        return ResponseEntity.ok(ConteudoMapper.paraDto(conteudo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
