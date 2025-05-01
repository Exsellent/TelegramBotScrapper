package backend.academy.bot.service;

import backend.academy.bot.dto.LinkResponse;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisCacheService {
    private final RedisTemplate<String, List<LinkResponse>> linkRedisTemplate;
    private final RedisTemplate<String, String> notificationRedisTemplate;

    @Autowired
    public RedisCacheService(
            RedisTemplate<String, List<LinkResponse>> linkRedisTemplate,
            RedisTemplate<String, String> notificationRedisTemplate) {
        this.linkRedisTemplate = linkRedisTemplate;
        this.notificationRedisTemplate = notificationRedisTemplate;
    }

    public List<LinkResponse> getLinks(Long chatId) {
        return linkRedisTemplate.opsForValue().get("tracked-links:" + chatId);
    }

    public void setLinks(Long chatId, List<LinkResponse> links) {
        linkRedisTemplate.opsForValue().set("tracked-links:" + chatId, links, 10, TimeUnit.MINUTES);
    }

    public void deleteLinks(Long chatId) {
        linkRedisTemplate.delete("tracked-links:" + chatId);
    }

    public Set<String> getNotificationKeys() {
        return notificationRedisTemplate.keys("notifications:*");
    }

    public List<String> getNotifications(Long chatId) {
        return notificationRedisTemplate.opsForList().range("notifications:" + chatId, 0, -1);
    }

    public void deleteNotifications(Long chatId) {
        notificationRedisTemplate.delete("notifications:" + chatId);
    }

    public void setNotificationMode(Long chatId, String mode) {
        notificationRedisTemplate.opsForValue().set("notification-mode:" + chatId, mode);
    }

    public void removeLink(Long chatId, String url) {
        List<LinkResponse> links = linkRedisTemplate.opsForValue().get("tracked-links:" + chatId);
        if (links != null) {
            links.removeIf(link -> link.getUrl().equals(url));
            if (links.isEmpty()) {
                linkRedisTemplate.delete("tracked-links:" + chatId);
            } else {
                linkRedisTemplate.opsForValue().set("tracked-links:" + chatId, links, 10, TimeUnit.MINUTES);
            }
        }
    }
}
