package net.chateaulore.roguesgallery;

final class Cover {
    final String id;
    final String title;
    final String url;
    final String fallbackUrl;

    Cover(String id, String title, String url, String fallbackUrl) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.fallbackUrl = fallbackUrl;
    }
}
