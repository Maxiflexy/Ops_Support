package util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

public class CookieUtil {

    public static Cookie getCookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            return Arrays.stream(((HttpServletRequest) request).getCookies()).filter(c -> c.getName().equals(name)).findFirst().orElse(null);
        }
        return null;
    }

    public static void setCookie(HttpServletRequest request, HttpServletResponse response, String name, Cookie newCookie) {
        Cookie cookie = getCookie(request, name);
        String sameSite = System.getProperty("same_site");
        if (cookie != null) {
            cookie.setHttpOnly(true);                // Prevent access from JavaScript
            cookie.setSecure(true);                  // Only send over HTTPS
            cookie.setPath("/");                     // Available throughout the site
            cookie.setMaxAge(7 * 24 * 60 * 60);
            cookie.setValue(newCookie.getValue());
        } else {
            cookie = newCookie;
        }
        CookieHelper.createSetCookieHeader(response, cookie, SameSite.valueOf(sameSite));
    }
}
