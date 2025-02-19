package backend.academy.bot.command;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ConversationManager {
    private final Map<Long, ConversationState> userStates = new ConcurrentHashMap<>();
    private final Map<Long, TrackingData> trackingData = new ConcurrentHashMap<>();

    public static class TrackingData {
        private String url;
        private List<String> tags;
        private Map<String, String> filters;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public Map<String, String> getFilters() {
            return filters;
        }

        public void setFilters(Map<String, String> filters) {
            this.filters = filters;
        }

        @Override
        public String toString() {
            return "TrackingData{" + "url='" + url + '\'' + ", tags=" + tags + ", filters=" + filters + '}';
        }
    }

    public ConversationState getUserState(Long chatId) {
        return userStates.getOrDefault(chatId, ConversationState.IDLE);
    }

    public void setUserState(Long chatId, ConversationState state) {
        userStates.put(chatId, state);
    }

    public TrackingData getTrackingData(Long chatId) {
        return trackingData.computeIfAbsent(chatId, k -> new TrackingData());
    }

    public void clearTrackingData(Long chatId) {
        trackingData.remove(chatId);
    }
}
