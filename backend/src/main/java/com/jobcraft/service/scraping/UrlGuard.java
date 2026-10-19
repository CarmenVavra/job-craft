package com.jobcraft.service.scraping;

import com.jobcraft.exception.BadRequestException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * Schützt den Scraper vor Server-Side Request Forgery: nur http(s) und keine internen Adressen.
 */
public final class UrlGuard {

    private UrlGuard() {
    }

    /**
     * @param url zu prüfende URL
     * @return geparste URI
     * @throws BadRequestException wenn die URL ungültig ist oder auf ein internes Netz zeigt
     */
    public static URI validate(String url) {
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Bitte gib einen gültigen Link ein (beginnend mit https://).");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!("http".equals(scheme) || "https".equals(scheme)) || uri.getHost() == null) {
            throw new BadRequestException("Bitte gib einen gültigen Link ein (beginnend mit https://).");
        }
        return uri;
    }

    /**
     * Löst den Host auf und verweigert Loopback-, Link-Local- und private Adressen.
     *
     * @param uri Ziel
     */
    public static void ensurePublicHost(URI uri) {
        try {
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isLoopbackAddress() || address.isAnyLocalAddress() || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                    throw new BadRequestException("Interne Adressen können nicht abgerufen werden.");
                }
            }
        } catch (UnknownHostException e) {
            throw new BadRequestException("Die Adresse " + uri.getHost() + " ist nicht erreichbar.");
        }
    }
}
