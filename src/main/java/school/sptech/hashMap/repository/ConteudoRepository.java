package school.sptech.hashMap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import school.sptech.hashMap.entity.Conteudo;

public interface ConteudoRepository extends JpaRepository<Conteudo, Long> {
}
