package school.sptech.hashMap.mapper;

import school.sptech.hashMap.dto.ConteudoRequisicaoDto;
import school.sptech.hashMap.dto.ConteudoRespostaDto;
import school.sptech.hashMap.entity.Conteudo;

public final class ConteudoMapper {

    private ConteudoMapper() {
    }

    public static Conteudo paraEntidade(ConteudoRequisicaoDto dto) {
        Conteudo conteudo = new Conteudo();
        conteudo.setTitulo(dto.titulo());
        conteudo.setCorpo(dto.corpo());
        conteudo.setAutor(dto.autor());
        return conteudo;
    }

    public static ConteudoRespostaDto paraDto(Conteudo conteudo) {
        return new ConteudoRespostaDto(conteudo.getId(), conteudo.getTitulo(),
                conteudo.getCorpo(), conteudo.getAutor(), conteudo.getAtualizadoEm());
    }
}
