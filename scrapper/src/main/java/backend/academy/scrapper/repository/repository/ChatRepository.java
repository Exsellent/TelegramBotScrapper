package backend.academy.scrapper.repository.repository;

import backend.academy.scrapper.domain.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<Chat, Long> {}
