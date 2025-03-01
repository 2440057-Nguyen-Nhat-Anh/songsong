package com.example.songsong;

public interface IDownload {
    // Downloads a file in parallel from multiple clients
    void downloadFile(String d_host, int d_port, String fileName);

    // Downloads a file sequentially from a single client for comparison
    void downloadSequential(String d_host, int d_port, String fileName);

    // Downloads a file in sequence from multiple clients
    void downloadSequentialAll(String d_host, int d_port, String fileName);
}
