package com.example.songsong;

public interface IClient {
    // registers with the directory server
    void registerDirectory(String d_host, int d_port) throws Exception;
    // startFileServer() starts a file server on the client -> file fragments
    void startFileServer(int port) throws Exception;
    // Notify that it's active
    void sendNotice(String d_host, int d_port);
}
