package school.sptech.hashMap.dto;

import java.time.LocalDateTime;

public record ConteudoRespostaDto(Long id, String titulo, String corpo, String autor,
                                 LocalDateTime atualizadoEm) {
}
