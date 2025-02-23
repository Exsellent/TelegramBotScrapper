package backend.academy.bot.utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

public class LinkParser {

    private LinkParser() {}

    /**
     * Проверяет, является ли предоставленная строка валидным URL.
     *
     * @param urlString Строка для проверки
     * @return true, если URL валиден; false в противном случае
     */
    public static boolean isValidURL(String urlString) {
        try {
            new URI(urlString).toURL();
            return true;
        } catch (URISyntaxException | IllegalArgumentException | MalformedURLException | NullPointerException e) {
            return false;
        }
    }
}
